package edu.usc.irds.ocrextractor;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;

import org.apache.commons.lang3.StringUtils;
import org.apache.tika.cli.TikaCLI;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

public class ExractSerialNumber {
	public void updateFile(String filename, ArrayList<JSONObject> list, JSONObject o) {
		FileWriter file;
		try {
			file = new FileWriter(filename);
			System.setOut(new PrintStream(System.out));		
			for(int i=0;i<list.size();i++) {
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
	    ArrayList<JSONObject> list = new ArrayList<JSONObject>(); 
	    JSONObject o = new JSONObject();
	    int count = 0;
	    
		try {
			JSONObject object = new JSONObject();
			
			reader = new BufferedReader(new FileReader (args[0]));
			while((line = reader.readLine()) != null) {
	    		try {
	    			if(line.endsWith(".jpg") || line.endsWith(".png")) {
	    				count ++;
	    				File tmpOut = new File(line.substring(0, line.lastIndexOf(".")) +".txt");
	    				System.setOut(new PrintStream(new BufferedOutputStream(new FileOutputStream(tmpOut))));
	    				
	    				String[] arg = {"-t",line};
	    		        TikaCLI.main(arg);
	    		        
	    		        BufferedReader tmpReader = new BufferedReader(new FileReader(tmpOut));
	    		        String text;
	    		        JSONArray obj = new JSONArray();
	    		        
	    		        while((text = tmpReader.readLine()) != null) {
	    		        	String arr[] = text.split(" ");
	    		        	
	    		        	for (int i = 0; i < arr.length; i++) {
	    		        		if(StringUtils.isAlphanumeric(arr[i]) && !StringUtils.isAlpha(arr[i])) {
	    		        			obj.add(arr[i]);
	        		        	}
							}
	    		        	
	    		        }

	    		        object.put("extracted_serials", obj);
		        		object.put("image_id",line);
		        		
		        		list.add(object);
		        		if(count == 10) {
		        			o.put("counterfeit_serials", list);
		        			this.updateFile(args[1], list, o);
		        			count = 0;
		        		}
		        		
	    		        object.clear();	
	    		        
    		        	System.setOut(new PrintStream(System.out));
    		        	System.out.println("Successfully parsed file: " + text);
	    		        
		        		if(tmpReader != null) {
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
			
			o.put("counterfeit_serials", list);
			this.updateFile(args[1], list, o);
			
			if(reader != null) {
				reader.close();
			}
			
		} catch (FileNotFoundException e1) {
			e1.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
