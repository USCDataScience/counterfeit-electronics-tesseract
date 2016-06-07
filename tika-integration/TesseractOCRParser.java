/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.tika.parser.ocr;

import javax.imageio.ImageIO;

import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.LogFactory;
import org.apache.tika.exception.TikaException;
import org.apache.tika.io.TemporaryResources;
import org.apache.tika.io.TikaInputStream;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.mime.MediaType;
import org.apache.tika.mime.MediaTypeRegistry;
import org.apache.tika.parser.AbstractParser;
import org.apache.tika.parser.CompositeParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.Parser;
import org.apache.tika.parser.external.ExternalParser;
import org.apache.tika.parser.image.ImageParser;
import org.apache.tika.parser.image.TiffParser;
import org.apache.tika.parser.jpeg.JpegParser;
import org.apache.tika.sax.EmbeddedContentHandler;
import org.apache.tika.sax.XHTMLContentHandler;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * TesseractOCRParser powered by tesseract-ocr engine. To enable this parser,
 * create a {@link TesseractOCRConfig} object and pass it through a
 * ParseContext. Tesseract-ocr must be installed and on system path or the path
 * to its root folder must be provided:
 * <p>
 * TesseractOCRConfig config = new TesseractOCRConfig();<br>
 * //Needed if tesseract is not on system path<br>
 * config.setTesseractPath(tesseractFolder);<br>
 * parseContext.set(TesseractOCRConfig.class, config);<br>
 * </p>
 *
 *
 */
public class TesseractOCRParser extends AbstractParser {
    private static final long serialVersionUID = -8167538283213097265L;
    private static final TesseractOCRConfig DEFAULT_CONFIG = new TesseractOCRConfig();
    private static final Set<MediaType> SUPPORTED_TYPES = Collections.unmodifiableSet(
            new HashSet<MediaType>(Arrays.asList(new MediaType[] {
                    MediaType.image("png"), MediaType.image("jpeg"), MediaType.image("tiff"),
                    MediaType.image("x-ms-bmp"), MediaType.image("gif")
            })));
    private static Map<String,Boolean> TESSERACT_PRESENT = new HashMap<String, Boolean>();

    @Override
    public Set<MediaType> getSupportedTypes(ParseContext context) {
        // If Tesseract is installed, offer our supported image types
        TesseractOCRConfig config = context.get(TesseractOCRConfig.class, DEFAULT_CONFIG);
        if (hasTesseract(config))
            return SUPPORTED_TYPES;

        // Otherwise don't advertise anything, so the other image parsers
        //  can be selected instead
        return Collections.emptySet();
    }

    private void setEnv(TesseractOCRConfig config, ProcessBuilder pb) {
        String tessdataPrefix = "TESSDATA_PREFIX";
        Map<String, String> env = pb.environment();

        if (!config.getTessdataPath().isEmpty()) {
            env.put(tessdataPrefix, config.getTessdataPath());
        }
        else if(!config.getTesseractPath().isEmpty()) {
            env.put(tessdataPrefix, config.getTesseractPath());
        }
    }

    public boolean hasTesseract(TesseractOCRConfig config) {
        // Fetch where the config says to find Tesseract
        String tesseract = config.getTesseractPath() + getTesseractProg();

        // Have we already checked for a copy of Tesseract there?
        if (TESSERACT_PRESENT.containsKey(tesseract)) {
            return TESSERACT_PRESENT.get(tesseract);
        }

        // Try running Tesseract from there, and see if it exists + works
        String[] checkCmd = { tesseract };
        boolean hasTesseract = ExternalParser.check(checkCmd);
        TESSERACT_PRESENT.put(tesseract, hasTesseract);
        return hasTesseract;
     
    }

    public boolean hasConvert(TesseractOCRConfig config) {
        // Fetch where the config says to find Convert Program
        String convert = config.getConvertProgPath() + getConvertProg();

        // Have we already checked for a copy of Convert Program there?
        if (TESSERACT_PRESENT.containsKey(convert)) {
            return TESSERACT_PRESENT.get(convert);
        }

        // Try running Convert program from there, and see if it exists + works
        String[] checkCmd = { convert };
        boolean hasConvert = ExternalParser.check(checkCmd);
        TESSERACT_PRESENT.put(convert, hasConvert);
        return hasConvert;
     
    }
    
    public void parse(Image image, ContentHandler handler, Metadata metadata, ParseContext context) throws IOException,
            SAXException, TikaException {

        TemporaryResources tmp = new TemporaryResources();
        FileOutputStream fos = null;
        TikaInputStream tis = null;
        try {
            int w = image.getWidth(null);
            int h = image.getHeight(null);
            BufferedImage bImage = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
            File file = tmp.createTemporaryFile();
            fos = new FileOutputStream(file);
            ImageIO.write(bImage, "png", fos);
            tis = TikaInputStream.get(file);
            parse(tis, handler, metadata, context);

        } finally {
            tmp.dispose();
            if (tis != null)
                tis.close();
            if (fos != null)
                fos.close();
        }

    }

    @Override
    public void parse(InputStream stream, ContentHandler handler, Metadata metadata, ParseContext context)
            throws IOException, SAXException, TikaException {
        TesseractOCRConfig config = context.get(TesseractOCRConfig.class, DEFAULT_CONFIG);
        // If Tesseract is not on the path with the current config, do not try to run OCR
        // getSupportedTypes shouldn't have listed us as handling it, so this should only
        //  occur if someone directly calls this parser, not via DefaultParser or similar
        if (! hasTesseract(config))
            return;

        TemporaryResources tmp = new TemporaryResources();
        try {
            TikaInputStream tikaStream = TikaInputStream.get(stream, tmp);

            XHTMLContentHandler xhtml = new XHTMLContentHandler(handler, metadata);
            xhtml.startDocument();
            File tmpImgFile = tmp.createTemporaryFile();
            parse(tikaStream, tmpImgFile, xhtml, config);
            // Temporary workaround for TIKA-1445 - until we can specify
            //  composite parsers with strategies (eg Composite, Try In Turn),
            //  always send the image onwards to the regular parser to have
            //  the metadata for them extracted as well
            _TMP_IMAGE_METADATA_PARSER.parse(tikaStream, new EmbeddedContentHandler(xhtml), metadata, context);
            xhtml.endDocument();
        } finally {
            tmp.dispose();
        }
    }

    /**
     * Use this to parse content without starting a new document.
     * This appends SAX events to xhtml without re-adding the metadata, body start, etc.
     * @param stream inputstream
     * @param xhtml handler
     * @param config TesseractOCRConfig to use for this parse
     * @throws IOException
     * @throws SAXException
     * @throws TikaException
     */
    public void parseInline(InputStream stream, XHTMLContentHandler xhtml, TesseractOCRConfig config)
            throws IOException, SAXException, TikaException {
        // If Tesseract is not on the path with the current config, do not try to run OCR
        // getSupportedTypes shouldn't have listed us as handling it, so this should only
        //  occur if someone directly calls this parser, not via DefaultParser or similar
        if (! hasTesseract(config))
            return;

        TemporaryResources tmp = new TemporaryResources();
        try {
            TikaInputStream tikaStream = TikaInputStream.get(stream, tmp);
            File tmpImgFile = tmp.createTemporaryFile();
            parse(tikaStream, tmpImgFile, xhtml, config);
        } finally {
            tmp.dispose();
        }

    }
    
    /**
     * A wrapper around processCommand method which takes streaming object as an argument.
     * Passing {@code null} will disable log streaming
     * @param cmd
     * @param config
     * @throws IOException
     * @throws TikaException
     */
    private void processCommand(String[] cmd, TesseractOCRConfig config) throws IOException, TikaException {
    	processCommand(cmd, config, null);
    }
    
    /**
     * This method is used to process any command related to Tesseract OCR.
     * @param cmd
     * @param config
     * @param streamingObject
     * @throws IOException 
     * @throws TikaException 
     */
    private void processCommand(String[] cmd, TesseractOCRConfig config, File streamingObject) throws IOException, TikaException {
    	ProcessBuilder pb = new ProcessBuilder(cmd);
        setEnv(config, pb);
        final Process process = pb.start();

        process.getOutputStream().close();

        if(streamingObject != null) {
        	InputStream out = process.getInputStream();
            InputStream err = process.getErrorStream();
        	logStream("OCR MSG", out, streamingObject);
	        logStream("OCR ERROR", err, streamingObject);
        }

        FutureTask<Integer> waitTask = new FutureTask<Integer>(new Callable<Integer>() {
            public Integer call() throws Exception {
                return process.waitFor();
            }
        });

        Thread waitThread = new Thread(waitTask);
        waitThread.start();

        try {
            waitTask.get(config.getTimeout(), TimeUnit.SECONDS);

        } catch (InterruptedException e) {
            waitThread.interrupt();
            process.destroy();
            Thread.currentThread().interrupt();
            throw new TikaException("TesseractOCRParser interrupted", e);

        } catch (ExecutionException e) {
            // should not be thrown

        } catch (TimeoutException e) {
            waitThread.interrupt();
            process.destroy();
            throw new TikaException("TesseractOCRParser timeout", e);
        }
    }
    
    private void processImage(File input, File processImgFile, TesseractOCRConfig config) throws IOException, TikaException {
    	String[] cmd = { "convert", input.getAbsolutePath(), " -threshold 50% -filter triangle -resize 200% ",
                processImgFile.getAbsolutePath() };
    	
    	processCommand(cmd, config, input);
    	
    }
    
    private void parse(TikaInputStream tikaInputStream, File tmpImgFile, XHTMLContentHandler xhtml, TesseractOCRConfig config)
            throws IOException, SAXException, TikaException {
        File tmpTxtOutput = null;
        TemporaryResources tmp = null;

        try {
            File input = tikaInputStream.getFile();
            long size = tikaInputStream.getLength();

            if (size >= config.getMinFileSizeToOcr() && size <= config.getMaxFileSizeToOcr()) {
            	
            	// Process image if Convert Tool is present
            	if(hasConvert(config)) {
	            	tmp = new TemporaryResources();
	            	File processImgFile = tmp.createTemporaryFile();
	            	processImage(input, processImgFile, config);
	            	input = processImgFile;
            	}
           
                doOCR(input, tmpImgFile, config);

                // Tesseract appends .txt to output file name
                tmpTxtOutput = new File(tmpImgFile.getAbsolutePath() + ".txt");

                if (tmpTxtOutput.exists()) {
                    try (InputStream is = new FileInputStream(tmpTxtOutput)) {
                        extractOutput(is, xhtml);
                    }
                }

            }

        } finally {
            if (tmpTxtOutput != null) {
                tmpTxtOutput.delete();
            }
            if(tmp != null)
            	tmp.close();
        }
    }

    // TIKA-1445 workaround parser
    private static Parser _TMP_IMAGE_METADATA_PARSER = new CompositeImageParser();
    private static class CompositeImageParser extends CompositeParser {
        private static final long serialVersionUID = -2398203346206381382L;
        private static List<Parser> imageParsers = Arrays.asList(new Parser[]{
                new ImageParser(), new JpegParser(), new TiffParser()
        });
        CompositeImageParser() {
            super(new MediaTypeRegistry(), imageParsers);
        }
    }

    /**
     * Run external tesseract-ocr process.
     *
     * @param input
     *          File to be ocred
     * @param output
     *          File to collect ocr result
     * @param config
     *          Configuration of tesseract-ocr engine
     * @throws TikaException
     *           if the extraction timed out
     * @throws IOException
     *           if an input error occurred
     */
    private void doOCR(File input, File output, TesseractOCRConfig config) throws IOException, TikaException {
        String[] cmd = { config.getTesseractPath() + getTesseractProg(), input.getPath(), output.getPath(), "-l",
                config.getLanguage(), "-psm", config.getPageSegMode() };

        processCommand(cmd, config, input); 

    }

    /**
     * Reads the contents of the given stream and write it to the given XHTML
     * content handler. The stream is closed once fully processed.
     *
     * @param stream
     *          Stream where is the result of ocr
     * @param xhtml
     *          XHTML content handler
     * @throws SAXException
     *           if the XHTML SAX events could not be handled
     * @throws IOException
     *           if an input error occurred
     */
    private void extractOutput(InputStream stream, XHTMLContentHandler xhtml) throws SAXException, IOException {

        xhtml.startElement("div", "class", "ocr");
        try (Reader reader = new InputStreamReader(stream, UTF_8)) {
            char[] buffer = new char[1024];
            for (int n = reader.read(buffer); n != -1; n = reader.read(buffer)) {
                if (n > 0)
                    xhtml.characters(buffer, 0, n);
            }
        }
        xhtml.endElement("div");

    }

    /**
     * Starts a thread that reads the contents of the standard output or error
     * stream of the given process to not block the process. The stream is closed
     * once fully processed.
     */
    private void logStream(final String logType, final InputStream stream, final File file) {
        new Thread() {
            public void run() {
                Reader reader = new InputStreamReader(stream, UTF_8);
                StringBuilder out = new StringBuilder();
                char[] buffer = new char[1024];
                try {
                    for (int n = reader.read(buffer); n != -1; n = reader.read(buffer))
                        out.append(buffer, 0, n);
                } catch (IOException e) {

                } finally {
                    IOUtils.closeQuietly(stream);
                }

                String msg = out.toString();
                LogFactory.getLog(TesseractOCRParser.class).debug(msg);
            }
        }.start();
    }

    static String getTesseractProg() {
        return System.getProperty("os.name").startsWith("Windows") ? "tesseract.exe" : "tesseract";
    }
    
    static String getConvertProg() {
    	return System.getProperty("os.name").startsWith("Windows") ? "convert.exe" : "convert";
    }

}
