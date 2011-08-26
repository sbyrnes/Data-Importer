package com.fogstack.data;

import java.util.ArrayList;

/** Parses a line of a CSV file into a String array, respecting the use of quotations to escape commas. */
public class CSVParser extends Parser{
	public String[] parseLine(String line)
	{
		ArrayList<String> fields = new ArrayList<String>();
		
		StringBuffer fieldBuffer = new StringBuffer();
		boolean ignoreCommas = false;
		for(int i = 0; i < line.length(); i++)
		{
			char c = line.charAt(i);
			switch(c)
			{
				case '"':	ignoreCommas = !ignoreCommas;
							break;
				case ',':	if(ignoreCommas) {
								fieldBuffer.append(c);
							} else { 
								fields.add(fieldBuffer.toString());
								fieldBuffer	= new StringBuffer();
							}
						 	break;
				default:	fieldBuffer.append(c);
			}
		}
		
		return fields.toArray(new String[fields.size()]);
	}
}
