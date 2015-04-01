package com.wdroome.altomsgs;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.io.InputStreamReader;
import java.util.List;

import com.wdroome.json.IJSONLexan;
import com.wdroome.json.JSONParseException;
import com.wdroome.json.JSONLexan;
import com.wdroome.json.JSONParser;
import com.wdroome.json.JSONValue_Object;

/**
 * Utility command to parse and validate ALTO json messages.
 * @author wdr
 */
public class ValidateALTOMsg
{
	
	/**
	 * Parse and validate ALTO json messages.
	 * The arguments are file names.
	 * The first line of each file is an ALTO media type,
	 * and the rest of the file is an ALTO message of that type.
	 * @param args The files to parse and validate.
	 * 		"-" means read standard input.
	 * @see MakeALTOMsg#validate(String,JSONValue_Object)			
	 */
	public static void main(String[] args)
	{
		int exitCode = 0;
		boolean singleFile = false;
		String indent = "  ";
		if (args.length == 0) {
			args = new String[] {"-"};
		}
		if (args.length == 1) {
			singleFile = true;
			indent = "";
		}
		for (String f:args) {
			if (!singleFile) {
				System.out.println();
				System.out.println(f + ":");
			}
			LineNumberReader rdr;
			try {
				if (f.equals("-")) {
					rdr = new LineNumberReader(new InputStreamReader(System.in));
				} else {
					rdr = new LineNumberReader(new FileReader(f));
				}
			} catch (FileNotFoundException e) {
				System.out.println(indent + "Cannot open '" + f + "'");
				exitCode = 1;
				continue;
			}
			String mediaType;
			try {
				mediaType = rdr.readLine().trim();
			} catch (IOException e) {
				System.out.println(indent + "Error reading file: " + e.toString());
				exitCode = 1;
				continue;
			}
			try {
				JSONValue_Object json = JSONParser.parseObject(new JSONLexan(rdr), false);
				List<String> errors = MakeALTOMsg.validate(mediaType, json);
				if (errors == null) {
					if (!singleFile) {
						System.out.println(indent + "Passed validation");
					}
				} else {
					if (!singleFile) {
						System.out.println(indent + errors.size() + " validation errors:");
					}
					for (String error: errors) {
						System.out.println(indent + error);
					}
					exitCode = 1;
				}
			} catch (JSONParseException e) {
				System.out.println(indent + e.toString());
				exitCode = 1;
			} catch (Exception e) {
				System.out.println(indent + "Error parsing JSON: " + e.toString());
				exitCode = 1;
				continue;
			}
			try {
				rdr.close();
			} catch (IOException e) {
				// Ignore
			}
		}
		System.exit(exitCode);
	}
}
