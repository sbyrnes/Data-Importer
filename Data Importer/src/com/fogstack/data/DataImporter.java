package com.fogstack.data;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/** 
 * Utility to import CSV files into a SQL database based on an XML mapping file.
 */
public class DataImporter {	
	static Map<String, String> csvFieldToDatabaseColumnMap = new HashMap<String, String>();
	static Map<String, String> csvFieldToTypeMap = new HashMap<String, String>();
	static List<String> csvFieldNames = new ArrayList<String>();

	static Map<String, Integer> csvFieldToColumnIndexMap = new HashMap<String, Integer>();
	
	static String outputTableName;
	static String databaseUrl;
	static String databaseUserName;
	static String databasePassword;
	static String inputType;
	
	static boolean verify = false;
	
	static final String STRING_TYPE = "string";
	static final String INT_TYPE = "int";
	static final String FLOAT_TYPE = "float";
	
	static final int LOGGING_INTERVAL = 10;
	
	public static void main(String[] args) {
		System.out.println("---------------------------------------------------");
		System.out.println("CSV to SQL importer");
		System.out.println("v0.1");
		System.out.println("copyright 2011 Fogstack LLC");
		System.out.println("---------------------------------------------------");
		
		if(args.length < 2) exitWithUsage();
		
		// read the mapping file name
		String mappingFileName = args[0];
		if(mappingFileName == null) exitWithUsage();
		System.out.println("Parsing mapping file: " + mappingFileName);
		
		List<String> errorsList = new ArrayList<String>();
		
		// read the CSV file name
		String csvFileName = args[1];
		if(csvFileName == null) exitWithUsage();
		System.out.println("Parsing mapping file: " + csvFileName);
		
		if(args.length == 3)
		{
			verify = true;
			System.out.println("Verification mode enabled");
		}
		
		try {
			// Process the mapping file
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			factory.setValidating(false);
			DocumentBuilder builder = factory.newDocumentBuilder();
			Document document = builder.parse(mappingFileName);
			NodeList schemaNodes = document.getElementsByTagName("schema-mapping");
			Element schemaElement = (Element)schemaNodes.item(0);
			
			outputTableName = schemaElement.getAttribute("outputTableName");
			databaseUrl = schemaElement.getAttribute("databaseUrl");
			databaseUserName = schemaElement.getAttribute("databaseUserName");
			databasePassword = schemaElement.getAttribute("databasePassword");
			inputType = schemaElement.getAttribute("inputFileFormat");
			
			NodeList fields = schemaElement.getElementsByTagName("field");
			for (int i = 0; i < fields.getLength(); i++) {
				Element fieldElement = (Element)fields.item(i);
				String csvColumnName = fieldElement.getAttribute("inputFieldName");
				String dbColumnName = fieldElement.getAttribute("outputFieldName");
				String fieldType = fieldElement.getAttribute("fieldType");
				
				csvFieldToDatabaseColumnMap.put(csvColumnName, dbColumnName);
				csvFieldToTypeMap.put(csvColumnName, fieldType);
				csvFieldNames.add(csvColumnName);				
			}
			
		} catch (Exception e) {
			e.printStackTrace();
		}

		File inputFile = new File(csvFileName);
		FileReader fr = null;
		BufferedReader br = null;
		Connection dbConn = null;
	    PreparedStatement insertRow = null;
	    
	    Parser parser = Parser.createParser(inputType);

		try {
			fr = new FileReader(inputFile);
			br =  new BufferedReader(fr);
			
			// Process the CSV File
			String firstLine = br.readLine();
			
			if(firstLine == null) System.exit(0);
			
			String[] csvColumns = parser.parseLine(firstLine);
			for(int i = 0; i < csvColumns.length; i++)
			{
				csvFieldToColumnIndexMap.put(csvColumns[i], i);
			}
						
			dbConn = getConnection();
			
			// create the insert prepared statement
			StringBuffer insertStmt = new StringBuffer();
			insertStmt.append("INSERT INTO ");
			insertStmt.append(outputTableName);
			insertStmt.append(" (");
			// for all of the CSV field names, create the corresponding database column insert values
			for(String csvColumnName : csvFieldNames) {
				insertStmt.append(csvFieldToDatabaseColumnMap.get(csvColumnName)).append(",");
			}
			insertStmt.deleteCharAt(insertStmt.length()-1);
			insertStmt.append(") VALUES (");
			for(int i=0; i < csvFieldNames.size(); i++) {
				insertStmt.append("?,");
			}
			insertStmt.deleteCharAt(insertStmt.length()-1);
			insertStmt.append(")");
			System.out.println("Using statement format: " + insertStmt.toString());
			insertRow = dbConn.prepareStatement(insertStmt.toString());

			long lineCount = 0;
			long insertCount = 0;
			long errorLineCount = 0;
			String line;
			while((line = br.readLine()) != null)
			{
				try {
					String[] fields = parser.parseLine(line);
					// for all of the CSV field names create the field insert statements
					int stmtIndex = 1;
					for(String csvColumnName : csvFieldNames) {
						String type = csvFieldToTypeMap.get(csvColumnName);
						int index = csvFieldToColumnIndexMap.get(csvColumnName);
						
						if(type.equals(STRING_TYPE)) {
							insertRow.setString(stmtIndex, fields[index]);
						} else if(type.equals(INT_TYPE)) {
							insertRow.setInt(stmtIndex, Integer.parseInt(fields[index]));
						} else if(type.equals(FLOAT_TYPE)) {
							insertRow.setFloat(stmtIndex, Float.parseFloat(fields[index]));
						} else {
							insertRow.setString(stmtIndex, fields[index]);
						}
						stmtIndex++;
					}
					
					if(!verify)
					{
						// perform the Database insert
						insertRow.executeUpdate();
						dbConn.commit();
						insertCount++;
					} else {
						System.out.println(insertRow.toString());
					}
					
					if(lineCount % LOGGING_INTERVAL == 0) System.out.println(lineCount + " lines processed");
					lineCount++;
				} catch (Exception e) {
					e.printStackTrace();
					errorLineCount++;
					errorsList.add(line + "\nERROR: ("+e.getClass().getName()+")" + e.getMessage());
				}
			}

			System.out.println(lineCount + " Records processed");
			System.out.println(insertCount + " Records added to database");
			System.out.println(errorLineCount + " Records skipped due to error");
			if(errorLineCount > 0)
			{
				for(String error : errorsList)
				{
					System.out.println("----------------------");
					System.out.println(error);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				//close out the db
				insertRow.close();
				dbConn.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
				
			try {
				// close out the input file
				br.close();
				fr.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
    public static Connection getConnection() throws SQLException {
	    Properties connectionProps = new Properties();
	    connectionProps.put("user", databaseUserName);
	    connectionProps.put("password", databasePassword);
	    Connection conn = DriverManager.
	        getConnection(databaseUrl, connectionProps);

	    System.out.println("Connected to database");
	    conn.setAutoCommit(false);
	    return conn;
	  }


	/** Exit the application with a message that describes how to properly use it. */
	public static void exitWithUsage()
	{
		System.out.println("Usage: java -cp dataImporter.jar DataImporter <mapping_filename> <input_csv_filename> [-verify]");
		System.exit(0);
	}
}
