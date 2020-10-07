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
import java.util.stream.Collectors;

import org.apache.log4j.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import ro.racai.robin.dialog.CType;
import ro.racai.robin.dialog.RDConcept;
import ro.racai.robin.dialog.RDSayings;

/**
 * @author Radu Ion  ({@code radu@racai.ro})
 * <p>The Romanian implementation using
 * <a href="http://relate.racai.ro:5000">RELATE</a>, the TEPROLIN web service.</p>
 */
public class RoTextProcessor extends TextProcessor {
	private static final String TEPROLIN_QUERY =
		"http://relate.racai.ro:5000/process";
	private static final Logger LOGGER = Logger.getLogger(RoTextProcessor.class.getName());
	private static final String UTF8_STRCONST = "UTF-8";

	public RoTextProcessor(Lexicon lex, WordNet wn, RDSayings say) {
		super(lex, wn, say);
	}
	
	/* (non-Javadoc)
	 * @see ro.racai.robin.nlp.TextProcessor#processText(java.lang.String)
	 */
	@Override
	protected List<Token> processText(String text) {
		StringBuilder content = new StringBuilder();
				
		try {
			URL url = new URL(RoTextProcessor.TEPROLIN_QUERY);
			URLConnection conn = url.openConnection();
			HttpURLConnection http = (HttpURLConnection) conn;
			
			http.setRequestMethod("POST");
			http.setDoOutput(true);
			
			Map<String,String> arguments = new HashMap<>();

			arguments.put("text", text);

			StringJoiner sj = new StringJoiner("&");
			
			for(Map.Entry<String,String> entry : arguments.entrySet()) {
				sj.add(URLEncoder.encode(entry.getKey(), UTF8_STRCONST) + "="
						+ URLEncoder.encode(entry.getValue(), UTF8_STRCONST));
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
				BufferedReader in = new BufferedReader(
						new InputStreamReader(http.getInputStream(), StandardCharsets.UTF_8));
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
			return new ArrayList<>();
		}
		catch (IOException ioe) {
			ioe.printStackTrace();
			return new ArrayList<>();
		}
		
		List<Token> tokens = new ArrayList<>();
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
		if (text != null && !text.isEmpty()) {
			// 1. Make first letter upper case.
			text = text.substring(0, 1).toUpperCase() + text.substring(1);
			
			// 2. Add '?' or '.' depending on the statement.
			String[] spaceTokens = text.split("\\s+");

			if (spaceTokens.length == 1) {
				text += ".";
			}
			else if (
				lexicon.isQuestionFirstWord(spaceTokens[0]) ||
				lexicon.isQuestionFirstWord(spaceTokens[1])
			) {
				text += "?";
			}
			else {
				text += ".";
			}
			
			// TODO: make proper names sentence-case...
		}
		
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
		List<String> queryWords =
			query
			.stream()
			.map(x -> x.wform)
			.collect(Collectors.toList());
		
		// -1. If hello, return quickly.
		if (sayings.userOpeningStatement(queryWords)) {
			result.queryType = QType.HELLO;
			return result;
		}
		
		// 0. If goodbye, return quickly.
		if (sayings.userClosingStatement(queryWords)) {
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
				
				List<Integer> belowIndexes = new ArrayList<>();
				List<Integer> nounPhraseIndexes = new ArrayList<>();
				
				belowIndexes.add(i + 1);
				treeUnder(query, belowIndexes, nounPhraseIndexes);
				nounPhraseIndexes.sort((Integer o1, Integer o2) -> o1.compareTo(o2));
				
				// -1 because all indexes are +1 to match
				// dependency parsing 1-based indexes
				List<Token> nounPhrase =
					nounPhraseIndexes
					.stream()
					//.filter((x) -> !lexicon.isFunctionalPOS(query.get(x - 1).POS))
					.map(x -> query.get(x - 1))
					.collect(Collectors.toList());
				Argument pArg = new Argument(nounPhrase, isQueryVariable(nounPhrase));
				
				result.predicateArguments.add(pArg);
			}
		}
		
		int fti = 0;
		
		// Skip non-interesting words at the beginning
		// of the user's sentence.
		while (
			fti < query.size() &&
			lexicon.isSkippablePOS(query.get(fti).POS)
		) {
			fti++;
		}
		
		if (fti >= query.size() - 1) {
			return null;
		}
		
		Token firstToken = query.get(fti);
		Token secondToken = query.get(fti + 1);
		
		// 3. Determine the query type.
		if (lexicon.isCommandVerb(result.actionVerb)) {
			result.queryType = QType.COMMAND;
		}
		else if (firstToken.lemma.equals("cine")) {
			result.queryType = QType.PERSON;
		}
		else if (firstToken.lemma.equals("ce")) {
			if (
				lexicon.isPureNounPOS(secondToken.POS) &&
				universeConcepts != null
			) {
				for (RDConcept c : universeConcepts) {
					if (
						c.isThisConcept(secondToken.lemma, wordNet) &&
						c.getType() != CType.WORD
					) {
						switch (c.getType()) {
						case PERSON:
							result.queryType = QType.PERSON;
							break;
						case LOCATION:
							result.queryType = QType.LOCATION;
							break;
						case TIME:
							result.queryType = QType.TIME;
							break;
						default:
							result.queryType = QType.WHAT;
						}
						
						if (result.queryType != null) {
							break;
						}
					}
				}
			}
			else {
				result.queryType = QType.WHAT;
			}
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
		List<Integer> addedHeads = new ArrayList<>();
		
		for (int h : checkHeads) {
			for (int i = 0; i < query.size(); i++) {
				Token t = query.get(i);
				int tIndex = i + 1;
				
				if (t.head == h && !checkHeads.contains(tIndex) && !storedHeads.contains(tIndex)
						&& !addedHeads.contains(tIndex)) {
					addedHeads.add(tIndex);
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
		return query.stream().map(x -> x.wform).collect(Collectors.joining(" "));
	}

	@Override
	public boolean isQueryVariable(List<Token> argument) {
		if (
			argument != null &&
			!argument.isEmpty()
		) {
			int firstIndex = 0;
			
			if (argument.get(0).POS.startsWith("S")) {
				// Remove first preposition, if it exists.
				firstIndex = 1;
			}
			
			// Relative pronoun/determiner/adverb
			if (
				argument.get(firstIndex).POS.length() >= 2 &&
				argument.get(firstIndex).POS.charAt(1) == 'w'
				
			) {
				return true;
			}
			
			if (
				argument.size() == 1 &&
				(
					argument.get(0).POS.startsWith("N") ||
					argument.get(0).POS.startsWith("Y") ||
					argument.get(0).POS.startsWith("M")
				)
			) {
				// If we have a single noun in the argument
				return true;
			}
		}
		
		return false;
	}

	@Override
	public String expandEntities(String text) {
		text = text.trim();

		Map<Integer, Pair<EntityType, Integer>> entities = lexicon.markEntities(text);
		Map<Integer, Pair<Integer, String>> replacements = new HashMap<>();

		// 1. Expand all found entities
		for (Map.Entry<Integer, Pair<EntityType, Integer>> e : entities.entrySet()) {
			int offset = e.getKey();
			Pair<EntityType, Integer> pair = e.getValue();
			EntityType eType = pair.getFirstMember();
			int length = pair.getSecondMember();
			String entityAtOffset = text.substring(offset, length);
			String eText;

			switch (eType) {
				case DATE:
					eText = lexicon.sayDate(entityAtOffset);
					replacements.put(offset, new Pair<>(length, eText));
					break;
				case TIME:
					eText = lexicon.sayTime(entityAtOffset);
					replacements.put(offset, new Pair<>(length, eText));
					break;
				case NUMBER:
					eText = lexicon.sayNumber(entityAtOffset);
					replacements.put(offset, new Pair<>(length, eText));
			}
		} // end all offsets

		// 2. Insert the replacements back into the original text
		List<Integer> allOffsets = new ArrayList<>();

		for (int offset : entities.keySet()) {
			if (allOffsets.isEmpty()) {
				allOffsets.add(offset);
			}
			else {
				// Insert offset in the sorted list,
				// on the proper position.
				boolean added = false;

				for (int i = 0; i < allOffsets.size(); i++) {
					if (offset <= allOffsets.get(i)) {
						allOffsets.add(i, offset);
						added = true;
						break;
					}
				}

				if (!added) {
					allOffsets.add(offset);
				}
			}
		}

		StringBuilder result = new StringBuilder();
		int walkIndex = 0;

		for (int i = 0; i < allOffsets.size(); i++) {
			int offset = allOffsets.get(i);
			String eText = replacements.get(offset).getSecondMember();
			int length = replacements.get(offset).getFirstMember();

			result.append(text.substring(walkIndex, offset));
			result.append(eText);
			walkIndex = offset + length;
		}
		
		if (result.length() == 0) {
			return text;
		}
		
		return result.toString();
	}
}
