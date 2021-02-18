import core.ExcelReader;

public class ExcelToMysql {

  public static void main(String args[]) {
    ExcelReader excelReader = new ExcelReader();
    try {
      excelReader.readExcel();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

}
