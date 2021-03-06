package core;

import static constants.AppConstants.BATCH_SIZE;
import static constants.AppConstants.FILE_EXTENSION;
import static constants.AppConstants.FILE_NAME;
import static constants.AppConstants.FILE_PATH;
import static constants.AppConstants.INCORRECT_FILE_FORMAT_EXCEPTION;
import java.util.LinkedHashMap;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import util.Util;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.Map;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

@Slf4j
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
        readSheet(databaseConfig, sheet, fileName);
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
        log.error("Exception in reading excel file", e);
        throw e;
      }
    }
  }

  private boolean validateFileName(String fileName) throws Exception {
    String ext = Util.getFileExtension(fileName);
    if (!(FILE_EXTENSION.equalsIgnoreCase(ext))) {
      log.error(INCORRECT_FILE_FORMAT_EXCEPTION);
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

  private  String createTable(String sheetName, ConfigLoader databaseConfig,
      Map<String, Cell> columnNameTypeMap) {
    String tableName = Util.removeSpecialCharacterFromName(sheetName);
    databaseConfig.createTable(tableName, columnNameTypeMap);
    return tableName;
  }

  private void readSheet(ConfigLoader databaseConfig, XSSFSheet sheet, String sheetName)
      throws SQLException {
    Map<String, Cell> columnNameTypeMap = getColumnNameCellMap(sheet);
    String tableName = createTable(sheetName,databaseConfig,columnNameTypeMap);

    PreparedStatement statement = databaseConfig
        .getPreparedStatement(columnNameTypeMap, tableName);
    insertDataInTable(statement,sheet,databaseConfig);

    log.info("Records inserted successfully in table ->" + tableName);
  }

  private void insertDataInTable(PreparedStatement statement, XSSFSheet sheet,
      ConfigLoader databaseConfig)
      throws SQLException {
    Iterator<Row> rowIterator = sheet.iterator();
    int count = 0;
    int batchSize = Integer.parseInt((String) databaseConfig.getProperties().get(BATCH_SIZE));
    rowIterator.next();
    while (rowIterator.hasNext()) {
      Row row = rowIterator.next();
      Iterator<Cell> cellIterator = row.cellIterator();
      readRowData(statement, cellIterator);
      statement.addBatch();
      count++;

      if (count % batchSize == 0) {
        log.info("Inserting batch of records : " + count);
        databaseConfig.insertData(statement);
      }
    }
    databaseConfig.insertData(statement);
  }


  private Map<String, Cell> getColumnNameCellMap(XSSFSheet sheet) {
    Map<String, Cell> columns = new LinkedHashMap<>();
    sheet.getRow(0).forEach(cell -> {
      columns.put(Util.removeSpecialCharacterFromName(cell.getStringCellValue()), cell);
    });
    return columns;
  }
}
