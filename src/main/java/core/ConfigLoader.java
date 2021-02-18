package core;

import static constants.AppConstants.CLOSING_BRACKET;
import static constants.AppConstants.COMMA;
import static constants.AppConstants.DOUBLE_DATA_TYPE;
import static constants.AppConstants.INSERT_INTO_QUERY;
import static constants.AppConstants.OPENING_BRACKET;
import static constants.AppConstants.QUESTION_MARK;
import static constants.AppConstants.SPACE;
import static constants.AppConstants.TABLE_NAME;
import static constants.AppConstants.VALUES;
import static constants.AppConstants.VARCHAR_DATA_TYPE;

import constants.AppConstants;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;

public class ConfigLoader {

  private static Connection conn = null;
  private static Properties properties;
  private Statement stmt = null;

  private static ConfigLoader databaseConfig;

  private ConfigLoader() {
    try {
      if (properties == null) {
        loadDataFromPropertiesFile();
      }
      if (conn == null || conn.isClosed()) {
        createConnection();
      }
    } catch (SQLException e) {
      e.printStackTrace();
    }
  }

  public static ConfigLoader getDatabaseConfig() {
    if (databaseConfig == null) {
      return new ConfigLoader();
    }
    return databaseConfig;
  }

  protected Properties getProperties() {
    return properties;
  }

  private void loadDataFromPropertiesFile() {
    properties = new Properties();
    String propFileName = AppConstants.PROPERTY_FILE;
    InputStream inputStream;
    inputStream = getClass().getClassLoader().getResourceAsStream(propFileName);
    try {
      if (inputStream != null) {
        properties.load(inputStream);
      }
    } catch (FileNotFoundException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private Connection createConnection() {
    final String DB_URL = properties.getProperty(AppConstants.DB_URL);
    final String USER = properties.getProperty(AppConstants.USERNAME);
    final String PASS = properties.getProperty(AppConstants.PASSWORD);
    System.out.println("Connecting to a selected database...");
    try {
      Class.forName(AppConstants.MY_SQL_DRIVER);
      conn = DriverManager.getConnection(DB_URL, USER, PASS);
      conn.setAutoCommit(false);
    } catch (SQLException e) {
      e.printStackTrace();
    } catch (ClassNotFoundException e) {
      e.printStackTrace();
    }

    System.out.println("Connected database successfully...");
    return conn;
  }

  protected void createTable(String tableName, Map<String, Integer> columns) {
    try {
      if (!tableExist(tableName.toLowerCase())) {
        System.out.println("Creating table in given database...");
        stmt = conn.createStatement();
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(AppConstants.CREATE_TABLE).append(SPACE).append(tableName)
            .append(AppConstants.OPENING_BRACKET);
        columns.forEach((k, v) -> {
          stringBuilder.append(k);
          stringBuilder.append(SPACE);
          stringBuilder.append(getColumnType(v));
          stringBuilder.append(SPACE);
          stringBuilder.append(COMMA);
        });
        stringBuilder.deleteCharAt(stringBuilder.toString().length() - 1);
        stringBuilder.append(AppConstants.CLOSING_BRACKET);
        stmt.executeUpdate(stringBuilder.toString());
        System.out.println("Created table in given database...");
      }
    } catch (SQLException e) {
      e.printStackTrace();
    }
  }

  protected PreparedStatement getPreparedStatement(Map<String, Integer> columnNames,
      String tableName) {
    StringBuilder insertQuery = new StringBuilder(INSERT_INTO_QUERY);
    insertQuery.append(tableName);
    insertQuery.append(OPENING_BRACKET);
    StringBuilder valuesString = new StringBuilder();
    valuesString.append(OPENING_BRACKET);
    columnNames.forEach((k, v) -> {
      insertQuery.append(k);
      insertQuery.append(COMMA);
      valuesString.append(QUESTION_MARK);
      valuesString.append(COMMA);
    });

    insertQuery.deleteCharAt(insertQuery.toString().length() - 1);
    valuesString.deleteCharAt(valuesString.toString().length() - 1);
    insertQuery.append(CLOSING_BRACKET);
    insertQuery.append(VALUES);
    valuesString.append(CLOSING_BRACKET);

    insertQuery.append(valuesString);
    PreparedStatement statement = null;
    try {
      statement = conn.prepareStatement(insertQuery.toString());
    } catch (SQLException e) {
      e.printStackTrace();
    }
    return statement;
  }

  protected void insertData(PreparedStatement statement) {
    try {
      statement.executeBatch();
    } catch (SQLException e) {
      e.printStackTrace();
    }
  }

  protected void commitData() {
    try {
      conn.commit();
      conn.close();
    } catch (SQLException e) {
      e.printStackTrace();
    }
  }

  private String getColumnType(Integer columnType) {
    switch (columnType) {
      case 0:
        return DOUBLE_DATA_TYPE;
      case 1:
        return VARCHAR_DATA_TYPE;
    }
    return VARCHAR_DATA_TYPE;
  }

  private static boolean tableExist(String tableName) throws SQLException {
    boolean tExists = false;
    try (ResultSet rs = conn.getMetaData().getTables(null, null, tableName, null)) {
      while (rs.next()) {
        String tName = rs.getString(TABLE_NAME);
        if (tName != null && tName.equals(tableName)) {
          tExists = true;
          break;
        }
      }
    }
    return tExists;
  }

}
