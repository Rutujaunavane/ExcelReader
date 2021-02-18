# ExcelReader
A Java Program to read data from and store the records in mysql by creating tables.

## Descrption
This utility reads the data from excel file and store the records in mysql table.

## Technologies Used
* JAVA 8 
* Apache POI library for reading the excel file.
* Mysql Database for storing the excel records.
* SLF4j for logging.
* Mysql connector for connecting to the database.

## Installations Needed
* JAVA 8 
* Maven

## How to use
* Checkout the project.
* Add the configurations in the config.properties file located in the main/java/resources folder.
* Run mvn clean install in the location of pom.xml file.
* Execute the class ExcelToMysql.

## Output
On execution of the code a table will be created in the mysql database with the name same as the name of the excel file.
The table will contain the records from the excel file.
