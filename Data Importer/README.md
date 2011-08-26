File Data Importer
=================
A very simple utility for importing a file in either CSV or tab-delimited format into a relational database. You provide the data file and an XML mapping file that describes how to map the data into the SQL table and it will import everything. 

This was motivated by the fact that the US government publishes a wealth of very interesting data but insists in doing it in CSV and tab-delimited formats which change every year. Analyzing data is a lot more fun than importing it so I built this to make mapping all the crazy formats into unified tabels easy.

Install
-------
	
	ant build.xml

Usage
-------

	java -cp dataImporter.jar DataImporter <mapping_filename> <input_csv_filename> [-verify]

The mapping_filename is the XML mapping file and the input_csv_filename is the data file. Specifying the optional -verify flag will test the import but not actually insert into the table so you can verify the mapping before you try it. 

For example, the following CSV data might be exported from a bank account:

	Date, Transaction Amount, Description
	8/1/11, "($1,000.00)", Rent
	8/2/11, "$2,000.00", Paycheck
	8/3/11, "($100.00), ATM Withdrawal
	
Let's say you want to import this into a table with the following definition:

	create table transactionTable (
	`date` varchar(255) NOT NULL,
	`transactionAmount` double DEFAULT NULL,
	`description` varchar(255) NOT NULL,
	);
	
You would create a mapping file of the following form:

	<?xml version="1.0" encoding="utf-8"?>
	<schema-mapping outputTableName="transactionTable" 
					databaseUrl="jdbc:mysql://localhost:3306/yourDatabaseName" 
					databaseUserName="dbUserName" 
					databasePassword="dbPassword">
		<field inputFieldName="Date" outputFieldName="date" fieldType="string"/>
		<field inputFieldName="Transaction Amount" outputFieldName="transactionAmount" fieldType="float"/>
		<field inputFieldName="Description" outputFieldName="description" fieldType="string"/>
	</schema-mapping>

Note that the databaseUrl, databaseUserName and databasePassword should be specific to your database. It is assumed that the driver for your database is already in the classpath. If not, add it to the -cp argument when executing. 

Currently, the only supported fieldTypes are int, string and float.

COPYRIGHT
---------
Copyright (c) 2011 Sean Byrnes @sbyrnes
Released under the MIT license