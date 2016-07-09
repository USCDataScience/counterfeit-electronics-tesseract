package edu.usc.irds.ocrextractor;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.tika.cli.TikaCLI;
import org.codehaus.jackson.JsonEncoding;
import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonGenerator;
import org.json.simple.JSONObject;

public class ExractSerialNumber {
	
	@Deprecated
	public void updateFile(String filename, ArrayList<JSONObject> list, JSONObject o) {
		FileWriter file;
		try {
			file = new FileWriter(filename);
			System.setOut(new PrintStream(System.out));
			for (int i = 0; i < list.size(); i++) {
				System.out.println(list.get(i));
			}

			file.write(o.toJSONString());
			file.write(System.lineSeparator());
			file.flush();
			file.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void writeToJSON(String[] args) {
		BufferedReader reader = null;
		String line = null;
		
		JsonFactory jFactory = new JsonFactory();
        JsonGenerator jGenerator = null;
		
		try {
			
			jGenerator = jFactory.createJsonGenerator(new File(args[1]), JsonEncoding.UTF8);
	        jGenerator.writeStartObject();
	        jGenerator.writeFieldName("counterfeit-electronics");
	        jGenerator.writeStartArray();
			
			reader = new BufferedReader(new FileReader(args[0]));
			while ((line = reader.readLine()) != null) {
				try {
					if (line.endsWith(".jpg") || line.endsWith(".png")) {
						File tmpOut = new File(line.substring(0, line.lastIndexOf(".")) + ".txt");
						System.setOut(new PrintStream(new BufferedOutputStream(new FileOutputStream(tmpOut))));

						String[] arg = { "-t", line };
						TikaCLI.main(arg);

						BufferedReader tmpReader = new BufferedReader(new FileReader(tmpOut));
						String text;
						List<String> serialNumbers = new ArrayList<String>();

						while ((text = tmpReader.readLine()) != null) {
							String arr[] = text.split(" ");

							for (int i = 0; i < arr.length; i++) {
								if (StringUtils.isAlphanumeric(arr[i]) && !StringUtils.isAlpha(arr[i])) {
									serialNumbers.add(arr[i]);
								}
							}

						}
						
						jGenerator.writeStartObject();
				        jGenerator.writeStringField("image_id", line);
				        jGenerator.writeFieldName("extracted_serials");
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
					}
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
