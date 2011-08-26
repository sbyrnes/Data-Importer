package com.fogstack.data;


/** Parses a line of a Tab-delimited file into a String array, respecting the use of quotations to escape commas. */
public class TabParser extends Parser{
	public String[] parseLine(String line)
	{
		return line.split("\t");
	}
}
