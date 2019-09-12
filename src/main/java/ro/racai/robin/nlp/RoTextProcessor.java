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
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import java.util.stream.Collectors;

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
/**
 * @author Radu Ion
 *
 */
public class RoTextProcessor extends TextProcessor {
	private static final String TEPROLIN_QUERY =
		"http://relate.racai.ro:5000/process";
	private static final Logger LOGGER = Logger.getLogger(RoTextProcessor.class.getName());
	private RoLexicon lexicon;
	
	public RoTextProcessor() {
		lexicon = new RoLexicon();
	}

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
				
				tokens.add(new Token(wordform, lemma, msd, head, deprel, false));
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
		if (query == null) {
			return null;
		}
		
		Query result = new Query();
		int actionVerbID = 0;
		List<String> queryWordsLC =
			query
			.stream()
			.map((x) -> x.wform.toLowerCase())
			.collect(Collectors.toList());
		
		// 0. If goodbye, return quickly.
		if (lexicon.isClosingStatement(queryWordsLC)) {
			result.queryType = QType.GOODBYE;
			return result;
		}
		
		// 1. Find the root of the sentence. This has to be a main verb.
		for (int i = 0; i < query.size(); i++) {
			Token t = query.get(i);
			
			if (t.head == 0 && t.POS.startsWith("Vm")) {
				result.actionVerb = t.lemma.toLowerCase();
				// These are 1-based.
				actionVerbID = i + 1;
				break;
			}
		}
		
		if (actionVerbID == 0) {
			LOGGER.error("Could not find an action verb in the query '" + queryToString(query) + "'");
			return null;
		}
		
		// 2. Find all arguments (first dependents) of the action verb.
		// We only consider "noun" arguments, e.g. nouns, pronouns, abbreviations, numerals, etc.
		for (int i = 0; i < query.size(); i++) {
			Token t = query.get(i);

			if (t.head == actionVerbID && lexicon.isNounPOS(t.POS)) {
				t.isActionVerbDependent = true;
				
				List<Integer> belowIndexes = new ArrayList<Integer>();
				List<Integer> nounPhraseIndexes = new ArrayList<Integer>();
				
				belowIndexes.add(i + 1);
				treeUnder(query, belowIndexes, nounPhraseIndexes);
				nounPhraseIndexes.sort(new Comparator<Integer>() {
					@Override
					public int compare(Integer o1, Integer o2) {
						return o1.compareTo(o2);
					}
				});
				
				// -1 because all indexes are +1 to match
				// dependency parsing 1-based indexes
				List<Token> nounPhrase =
					nounPhraseIndexes
					.stream()
					.filter((x) -> !lexicon.isFunctionalPOS(query.get(x - 1).POS))
					.map((x) -> query.get(x - 1))
					.collect(Collectors.toList());
				
				result.predicateArguments.add(nounPhrase);
			}
		}
		
		if (query.size() < 2) {
			return null;
		}
		
		Token firstToken = query.get(0);
		
		if (lexicon.isPrepositionPOS(firstToken.POS)) {
			// Skip the first preposition in the query,
			// if it exists.
			firstToken = query.get(1);
		}
		
		// 3. Determine the query type.
		if (lexicon.isCommandVerb(result.actionVerb)) {
			result.queryType = QType.COMMAND;
		}
		else if (firstToken.lemma.equals("cine")) {
			result.queryType = QType.PERSON;
		}
		else if (firstToken.lemma.equals("ce")) {
			result.queryType = QType.WHAT;
		}
		else if (firstToken.lemma.equals("unde")) {
			result.queryType = QType.LOCATION;
		}
		else if (firstToken.lemma.equals("când")) {
			result.queryType = QType.TIME;
		}
		else if (firstToken.lemma.equals("cum")) {
			result.queryType = QType.HOW;
		}
		else {
			result.queryType = QType.YESNO;
		}
		
		return result;
	}
	
	/**
	 * Extracts the portion of the sentence under the
	 * dependency tree rooted at index.
	 * @param query            the list of tokens to use
	 * @param checkHeads       list of heads to search for
	 *                         in the dependency tree; initially
	 *                         it only contains the root index
	 * @param storedHeads       list of heads that are "below"
	 *                         root index                       
	 * @return                 a list of integers for indexes
	 *                         that are "below" the starting index
	 */
	private void treeUnder(List<Token> query, List<Integer> checkHeads, List<Integer> storedHeads) {
		List<Integer> addedHeads = new ArrayList<Integer>();
		
		for (int h : checkHeads) {
			for (int i = 0; i < query.size(); i++) {
				Token t = query.get(i);
				int tIndex = i + 1;
				
				if (t.head == h) {
					if (
						!checkHeads.contains(tIndex) &&
						!storedHeads.contains(tIndex) &&
						!addedHeads.contains(tIndex)
					) {
						addedHeads.add(tIndex);
					}
				}
			}
		}
		
		storedHeads.addAll(checkHeads);
		
		if (!addedHeads.isEmpty()) {
			treeUnder(query, addedHeads, storedHeads);
		}
	}
	
	// Debugging method.
	private String queryToString(List<Token> query) {
		return query.stream().map((x) -> x.wform).collect(Collectors.joining(" "));
	}
}
