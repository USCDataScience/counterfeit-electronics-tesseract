package edu.usc.irds.ocrextractor;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.tika.cli.TikaCLI;
import org.codehaus.jackson.JsonEncoding;
import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonGenerator;

class compareSerialNums implements Comparator<String> {

	@Override
	public int compare(String o1, String o2) {
		return o2.compareTo(o1);
	}	
}

public class ExractSerialNumber {

	public void writeToJSON(String[] args) {
		BufferedReader reader = null;
		String line = null;
		
		JsonFactory jFactory = new JsonFactory();
        JsonGenerator jGenerator = null;
		
		try {
			
			jGenerator = jFactory.createJsonGenerator(new File(args[1]), JsonEncoding.UTF8);
	        jGenerator.writeStartObject();
	        jGenerator.writeFieldName("counterfeit_serials");
	        jGenerator.writeStartArray();
			
			reader = new BufferedReader(new FileReader(args[0]));
			while ((line = reader.readLine()) != null) {
				try {
//					if (line.endsWith(".jpg") || line.endsWith(".png")) {
						String filename = line.replaceFirst("^file:", "");
						File tmpOut = new File(filename + ".txt");
						System.setOut(new PrintStream(new BufferedOutputStream(new FileOutputStream(tmpOut))));

						String[] arg = { "-t", line };
						TikaCLI.main(arg);

						BufferedReader tmpReader = new BufferedReader(new FileReader(tmpOut));
						String text;
						List<String> serialNumbers = new ArrayList<String>();

						while ((text = tmpReader.readLine()) != null) {
							String arr[] = text.split(" ");

							for (int i = 0; i < arr.length; i++) {
								if (StringUtils.isAlphanumeric(arr[i]) && !StringUtils.isAlpha(arr[i]) && arr[i].length() >= 5) {
									serialNumbers.add(arr[i]);
								}
							}

						}

						jGenerator.writeStartObject();
				        jGenerator.writeStringField("image_id", line);
				        jGenerator.writeFieldName("extracted_serials");
				        Collections.sort(serialNumbers, new compareSerialNums());
				        jGenerator.writeStartArray();
				        
				        for(String serialNumber: serialNumbers)
				        	jGenerator.writeString(serialNumber);
				        
				        jGenerator.writeEndArray();
				        jGenerator.writeEndObject();

						System.setOut(new PrintStream(System.out));
						System.out.println("Successfully parsed file: " + text);

						if (tmpReader != null) {
							tmpReader.close();
						}
//					}
				} catch (FileNotFoundException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}

			if (reader != null) {
				reader.close();
			}

		} catch (FileNotFoundException e1) {
			e1.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		finally {
			try {
				if(jGenerator != null) {
					jGenerator.writeEndArray();
			        jGenerator.writeEndObject();
					jGenerator.close();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}
