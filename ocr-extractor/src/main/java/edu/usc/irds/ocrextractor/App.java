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
    	ExractSerialNumber extractSerialNumber = new ExractSerialNumber();
    	extractSerialNumber.writeToJSON(args);
    }
}
