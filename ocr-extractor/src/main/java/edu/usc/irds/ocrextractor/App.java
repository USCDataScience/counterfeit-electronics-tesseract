package edu.usc.irds.ocrextractor;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collection;

import org.apache.commons.io.FileUtils;
import org.apache.tika.io.TemporaryResources;

public class App 
{
    public static void main( String[] args ) throws Exception
    {	
    	TemporaryResources tmp = new TemporaryResources();
    	File tmpFile = new File("/Users/zaranaparekh1/Documents/final_training_data/filenames.txt");
    	BufferedWriter bw = null;
    	
    	try {
			//tmpFile = tmp.createTemporaryFile();
			bw = new BufferedWriter(new FileWriter(tmpFile.getAbsolutePath(), false));
			File file = new File(args[0]);       
			Collection<File> files = FileUtils.listFiles(file, null, true);     
			for(File file2 : files){
				if(file2.getAbsolutePath().endsWith("jpg") || file2.getAbsolutePath().endsWith("png")) {
				    bw.write(file2.getAbsolutePath());
				    bw.newLine();
				    bw.flush();
				}
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
    	
    	if(tmp != null) {
    		tmp.close();
    	}
    	
    	ExractSerialNumber extractSerialNumber = new ExractSerialNumber();
    	extractSerialNumber.writeToJSON(new String[]{tmpFile.getAbsolutePath(),args[1]});
    }
}
