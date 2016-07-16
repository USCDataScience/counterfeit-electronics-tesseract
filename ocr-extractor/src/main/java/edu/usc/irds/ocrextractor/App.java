package edu.usc.irds.ocrextractor;

public class App 
{
    public static void main( String[] args ) throws Exception
    {	  	
    	ExractSerialNumber extractSerialNumber = new ExractSerialNumber();
    	extractSerialNumber.writeToJSON(args);
    }
}
