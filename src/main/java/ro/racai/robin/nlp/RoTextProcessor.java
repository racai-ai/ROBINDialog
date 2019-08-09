/**
 * 
 */
package ro.racai.robin.nlp;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

import org.apache.log4j.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

/**
 * @author Radu Ion  ({@code radu@racai.ro})
 * <p>The Romanian implementation using relate.racai.ro:5000,
 * the TEPROLIN web service.</p>
 */
public class RoTextProcessor extends TextProcessor {
	private static final String TEPROLIN_QUERY =
		"http://relate.racai.ro:5000/process";
	private static final Logger LOGGER = Logger.getLogger(RoTextProcessor.class.getName());

	/* (non-Javadoc)
	 * @see ro.racai.robin.nlp.TextProcessor#processText(java.lang.String)
	 */
	@Override
	protected List<Token> processText(String text) {
		StringBuffer content = new StringBuffer();
				
		try {
			URL url = new URL(RoTextProcessor.TEPROLIN_QUERY);
			URLConnection conn = url.openConnection();
			HttpURLConnection http = (HttpURLConnection) conn;
			
			http.setRequestMethod("POST");
			http.setDoOutput(true);
			
			Map<String,String> arguments = new HashMap<String, String>();

			arguments.put("text", text);

			StringJoiner sj = new StringJoiner("&");
			
			for(Map.Entry<String,String> entry : arguments.entrySet()) {
			    sj.add(URLEncoder.encode(entry.getKey(), "UTF-8") + "=" 
			         + URLEncoder.encode(entry.getValue(), "UTF-8"));
			}
			
			byte[] out = sj.toString().getBytes(StandardCharsets.UTF_8);
			int length = out.length;

			http.setFixedLengthStreamingMode(length);
			http.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
			http.connect();
			
			OutputStream os = http.getOutputStream();

			os.write(out);
			os.close();
			
			int status = http.getResponseCode();
			
			if (status == 200) {
				BufferedReader in =
					new BufferedReader(
						new InputStreamReader(http.getInputStream(), "UTF-8"));
				String line = in.readLine();
				
				
				while (line != null) {
					content.append(line);
					line = in.readLine();
				}
				
				in.close();
			}
			else {
				LOGGER.error("TEPROLIN query error for text '" + text + "'; error code " + status); 
			}
		}
		catch (UnsupportedEncodingException uee) {
			uee.printStackTrace();
			return null;
		}
		catch (IOException ioe) {
			ioe.printStackTrace();
			return null;
		}
		
		List<Token> tokens = new ArrayList<Token>();
		String json = content.toString();
		JSONParser parser = new JSONParser();
		
		try {
			JSONObject root = (JSONObject) parser.parse(json);
			JSONObject result = (JSONObject) root.get("teprolin-result");
			JSONArray tokenized = (JSONArray) result.get("tokenized");
			JSONArray tokenizedData = (JSONArray) tokenized.get(0);
						
			for (int i = 0; i < tokenizedData.size(); i++) {
				JSONObject tk = (JSONObject) tokenizedData.get(i);
				String wordform = (String) tk.get("_wordform");
				String lemma = (String) tk.get("_lemma");
				String msd = (String) tk.get("_msd");
				int head = ((Long) tk.get("_head")).intValue();
				String deprel = (String) tk.get("_deprel");
				
				tokens.add(new Token(wordform, lemma, msd, head, deprel));
			}
		}
		catch (ParseException pe) {
			pe.printStackTrace();
		}
				
		return tokens;
	}

	/* (non-Javadoc)
	 * @see ro.racai.robin.nlp.TextProcessor#textCorrection(java.lang.String)
	 */
	@Override
	public String textCorrection(String text) {
		// TODO: apply any text correction mechanisms here!
		return text;
	}

	/* (non-Javadoc)
	 * @see ro.racai.robin.nlp.TextProcessor#queryAnalyzer(java.util.List)
	 */
	@Override
	public Query queryAnalyzer(List<Token> query) {
		// TODO Auto-generated method stub
		return null;
	}
}
