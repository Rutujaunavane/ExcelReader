package core;

import static constants.AppConstants.BATCH_SIZE;
import static constants.AppConstants.FILE_EXTENSION;
import static constants.AppConstants.FILE_NAME;
import static constants.AppConstants.FILE_PATH;
import static constants.AppConstants.INCORRECT_FILE_FORMAT_EXCEPTION;

import util.Util;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

public class ExcelReader {

  public void readExcel() throws IOException {
    ConfigLoader databaseConfig = ConfigLoader.getDatabaseConfig();
    FileInputStream file = null;
    XSSFWorkbook workbook = null;
    try {
      String fileName = databaseConfig.getProperties().getProperty(FILE_NAME);
      String filePath = databaseConfig.getProperties().getProperty(FILE_PATH);
      if (validateFileName(fileName)) {
        file = new FileInputStream(new File((filePath + fileName)));
        workbook = new XSSFWorkbook(file);
        XSSFSheet sheet = workbook.getSheetAt(0);
        readExcel(databaseConfig, sheet, fileName);
      }
    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      try {
        databaseConfig.commitData();
        if (workbook != null) {
          workbook.close();
        }
        if (file != null) {
          file.close();
        }
      } catch (Exception e) {
        throw e;
      }
    }
  }

  private boolean validateFileName(String fileName) throws Exception {
    String ext = Util.getFileExtension(fileName);
    if (!(FILE_EXTENSION.equalsIgnoreCase(ext))) {
      throw new Exception(INCORRECT_FILE_FORMAT_EXCEPTION);
    }
    return true;
  }

  private void readRowData(PreparedStatement statement, Iterator<Cell> cellIterator)
      throws SQLException {
    while (cellIterator.hasNext()) {
      Cell cell = cellIterator.next();
      switch (cell.getCellType()) {
        case Cell.CELL_TYPE_NUMERIC:
          statement.setInt(cell.getColumnIndex() + 1, (int) cell.getNumericCellValue());
          break;
        case Cell.CELL_TYPE_STRING:
          statement.setString(cell.getColumnIndex() + 1, cell.getStringCellValue());
          break;
      }
    }
  }

  private void readExcel(ConfigLoader databaseConfig, XSSFSheet sheet, String fileName)
      throws SQLException {
    Map<String, Integer> columnNameTypeMap = getColumnNameTypeMap(sheet);
    String tableName = Util.getNameWithoutExtension(fileName);
    databaseConfig.createTable(tableName, columnNameTypeMap);
    Iterator<Row> rowIterator = sheet.iterator();
    rowIterator.next();
    PreparedStatement statement = databaseConfig
        .getPreparedStatement(columnNameTypeMap, tableName);
    int count = 0;
    int batchSize = Integer.parseInt((String) databaseConfig.getProperties().get(BATCH_SIZE));

    while (rowIterator.hasNext()) {
      Row row = rowIterator.next();
      Iterator<Cell> cellIterator = row.cellIterator();
      readRowData(statement, cellIterator);
      statement.addBatch();
      count++;

      if (count % batchSize == 0) {
        System.out.println("Inserting batch of records");
        databaseConfig.insertData(statement);
      }
    }
    databaseConfig.insertData(statement);
    System.out.println("Records inserted successfully in table ->" + tableName);
  }


  private Map<String, Integer> getColumnNameTypeMap(XSSFSheet sheet) {
    Map<String, Integer> columns = new HashMap<>();
    sheet.getRow(0).forEach(cell -> {
      columns.put(cell.getStringCellValue(), cell.getColumnIndex());
    });
    return columns;
  }
}
