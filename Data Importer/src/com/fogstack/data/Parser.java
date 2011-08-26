package com.fogstack.data;

/** Abstract representation of a parser. Supports a factory pattern to generate parser instances. */
public abstract class Parser {
	public static String CSV = "csv";
	public static String TAB = "tab";
	
	public static Parser createParser(String type)
	{
		if(type.equalsIgnoreCase("csv")) return new CSVParser();
		if(type.equalsIgnoreCase("tab")) return new TabParser();
		
		return null;
	}
	
	public abstract String[] parseLine(String line);
}
