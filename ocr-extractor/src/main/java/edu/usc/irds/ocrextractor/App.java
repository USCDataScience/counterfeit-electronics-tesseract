package edu.usc.irds.ocrextractor;

import org.apache.tika.cli.TikaCLI;

/**
 * Hello world!
 *
 */
public class App 
{
    public static void main( String[] args ) throws Exception
    {
    	// Dummy call to TikaCLI
    	//TODO: Change this to use Tika OCR and get parsed content from it
        TikaCLI cli = new TikaCLI();
        cli.main(args);
    }
}
