package edu.usc.irds.ocrextractor;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.common.SolrInputDocument;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class SolrUpdator {
	private File configFile;
	
	public void updateIndex(File jsonFile) {
		Properties props = new Properties();
		String solrUrl = props.getProperty("solr.url");
        HttpSolrClient client = new HttpSolrClient(solrUrl);
        SolrInputDocument sdoc = new SolrInputDocument();
        JSONParser parser = new JSONParser();
        Object object = null;
        
        try (InputStream stream = new FileInputStream(configFile)){
            props.load(stream);
            object = parser.parse(new FileReader(jsonFile.getAbsolutePath()));
            JSONObject jsonObject = (JSONObject) object;
            JSONArray objectArray = (JSONArray) jsonObject.get("counterfeit_serials");
            // iterator
            for (int i = 0; i < objectArray.size(); i++) {
				JSONObject obj = (JSONObject) objectArray.get(i);
				final List<String> serialNumbers = (List<String>) obj.get("extracted_serials");
				Map<String,Object> newField = new HashMap<String, Object>(){{put("extracted_serials", serialNumbers);}};
				sdoc.addField("", newField);
			}
            client.add(sdoc);
            client.close();
        } catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ParseException e) {
			e.printStackTrace();
		} catch (SolrServerException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
