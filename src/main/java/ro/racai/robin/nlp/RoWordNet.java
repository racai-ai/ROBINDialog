/**
 * 
 */
package ro.racai.robin.nlp;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

/**
 * @author Radu Ion ({@code radu@racai.ro})
 * <p>The Romanian WordNet class, implementing
 * the {@link WordNet} interface.</p>
 */
public class RoWordNet implements WordNet {
	private static final String WORDNET_QUERY =
		"https://relate.racai.ro/index.php?path=rownws&word=#WORD#&sid=#ILI#&wn=ro";
	private static final Logger LOGGER = Logger.getLogger(RoWordNet.class.getName());
	private Map<String, Boolean> wnEqualsCache = new HashMap<String, Boolean>();
	
	@Override
	public List<String> getHypernyms(String word) {
		return getRelationMembers(word, "hypernym");
	}

	@Override
	public List<String> getHyponyms(String word) {
		return getRelationMembers(word, "hyponym");
	}
	
	private List<String> getRelationMembers(String word, String relName) {
		List<String> members = new ArrayList<String>();
		String json = jsonWordNetResponse(word);
		JSONParser parser = new JSONParser();
		
		try {
			JSONObject root = (JSONObject) parser.parse(json);
			JSONArray senses = (JSONArray) root.get("senses");
			
			for (int i = 0; i < senses.size(); i++) {
				JSONObject sense = (JSONObject) senses.get(i);
				JSONArray relations = (JSONArray) sense.get("relations");
				
				for (int j = 0; j < relations.size(); j++) {
					JSONObject relation = (JSONObject) relations.get(j);
					String relationName = (String) relation.get("rel");
					
					if (relationName.equals(relName)) {
						members.add((String) relation.get("tliteral"));
					}
				}
			}
		}
		catch (ParseException pe) {
			pe.printStackTrace();
		}

		return members;
	}

	@Override
	public List<String> getSynonyms(String word) {
		List<String> synonyms = new ArrayList<String>();
		String json = jsonWordNetResponse(word);
		JSONParser parser = new JSONParser();
		
		try {
			JSONObject root = (JSONObject) parser.parse(json);
			JSONArray senses = (JSONArray) root.get("senses");
			
			for (int i = 0; i < senses.size(); i++) {
				JSONObject sense = (JSONObject) senses.get(i);
				String[] synset = ((String) sense.get("literal")).split(",");
				
				for (String syn : synset) {
					if (!syn.equals(word)) {
						synonyms.add(syn);
					}
				}
			}
		}
		catch (ParseException pe) {
			pe.printStackTrace();
		}

		return synonyms;
	}
	
	private String jsonWordNetResponse(String word) {
		String query = new String(RoWordNet.WORDNET_QUERY);

		try {
			word = URLEncoder.encode(word, "UTF-8");
		}
		catch (UnsupportedEncodingException uee) {
			uee.printStackTrace();
			return null;
		}
		
		query = query.replace("#WORD#", word);
		query = query.replace("#ILI#", "");
		
		try {
			URL url = new URL(query);
			HttpURLConnection con = (HttpURLConnection) url.openConnection();
			
			con.setRequestMethod("GET");
			con.setRequestProperty("Content-Type", "application/json");
			
			int status = con.getResponseCode();
			
			if (status == 200) {
				BufferedReader in =
					new BufferedReader(
						new InputStreamReader(con.getInputStream(), "UTF-8"));
				String line = in.readLine();
				StringBuffer content = new StringBuffer();
				
				while (line != null) {
					content.append(line);
					line = in.readLine();
				}
				
				in.close();
				return content.toString();
			}
			else {
				LOGGER.error("RELATE query error for word '" + word + "'; error code " + status);
			}
		}
		catch (IOException ioe) {
			ioe.printStackTrace();
		}
		
		return null;
	}

	@Override
	public boolean wordnetEquals(String w1, String w2) {
		String key12 = w1 + "#" + w2;
		String key21 = w2 + "#" + w1;
		
		if (wnEqualsCache.containsKey(key12)) {
			return wnEqualsCache.get(key12);
		}

		if (wnEqualsCache.containsKey(key21)) {
			return wnEqualsCache.get(key21);
		}
		
		// Synonym check with WordNet
		for (String syn : getSynonyms(w1)) {
			if (w2.equals(syn)) {
				wnEqualsCache.put(key12, true);
				wnEqualsCache.put(key21, true);
				
				return true;
			}
		}
		
		// Use hypernyms from WordNet (only direct hypernyms)
		for (String hyper : getHypernyms(w1)) {
			if (w2.equals(hyper)) {
				wnEqualsCache.put(key12, true);
				wnEqualsCache.put(key21, true);
				
				return true;
			}
		}
		
		// Use hyponyms from WordNet (only direct hyponyms)
		for (String hypo : getHyponyms(w1)) {
			if (w2.equals(hypo)) {
				wnEqualsCache.put(key12, true);
				wnEqualsCache.put(key21, true);
				
				return true;
			}
		}
		
		wnEqualsCache.put(key12, false);
		wnEqualsCache.put(key21, false);
		
		return false;
	}
}
