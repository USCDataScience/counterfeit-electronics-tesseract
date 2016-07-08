package edu.usc.irds.ocrextractor;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.SystemUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.tika.cli.TikaCLI;
import org.apache.tika.io.TemporaryResources;

public class App 
{
    public static void main( String[] args ) throws Exception
    {
    	TemporaryResources tmp = new TemporaryResources();
		BufferedReader reader = null;
		BufferedWriter bw = null;
		File tmpFile = null;
	    String line = null;
		
		try {
			tmpFile = tmp.createTemporaryFile();
			bw = new BufferedWriter(new FileWriter(tmpFile.getAbsolutePath(), true));
			File file = new File(args[0]);       
			Collection<File> files = FileUtils.listFiles(file, null, true);     
			for(File file2 : files){
			    bw.write(file2.getAbsolutePath());
			    bw.newLine();
			    bw.flush();
			} 
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (bw != null) try {
			    bw.close();
			 } catch (IOException ioe) {
				ioe.printStackTrace();
			 }
		}
	
		reader = new BufferedReader(new FileReader (tmpFile));
		while((line = reader.readLine()) != null) {
    		try {
    			if(line.endsWith(".jpg") || line.endsWith(".png")) {
    				System.setOut(new PrintStream(new BufferedOutputStream(new FileOutputStream(line.substring(0, line.lastIndexOf("."))+".txt"))));
    				String[] arg = {"-t",line};
    		        TikaCLI.main(arg);
    		        File tmpOut = new File(line.substring(0, line.lastIndexOf(".")) +".txt");
    		        BufferedReader tmpReader = new BufferedReader(new FileReader(tmpOut));
    		        String text;
    		        bw = new BufferedWriter(new FileWriter(new File(line.substring(0, line.lastIndexOf(".")) +"_final.txt")));
    		        while((text = tmpReader.readLine()) != null) {
    		        	String arr[] = text.split(" ");
    		        	for (int i = 0; i < arr.length; i++) {
    		        		if(StringUtils.isAlphanumeric(arr[i]) && !StringUtils.isAlpha(arr[i])) {
        		        		bw.write(arr[i]);
        		        		bw.newLine();
        		        	}
						}
    		        }
    		        bw.flush();
	        		bw.close();
	        		tmpOut.delete();
	        		if(tmpReader != null) {
	        			tmpReader.close();
	        		}
    			}
    		} catch (FileNotFoundException e) {
    			e.printStackTrace();
    		} 
		}
		if(reader != null) {
			reader.close();
		}
    }
}
