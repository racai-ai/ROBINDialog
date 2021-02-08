/**
 * 
 */
package ro.racai.robin.nlp;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.InvocationTargetException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
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
import ro.racai.robin.dialog.RDResponseGenerator;
import ro.racai.robin.dialog.RDSayings;

/**
 * @author Radu Ion ({@code radu@racai.ro})
 *         <p>
 *         The Romanian implementation using <a href="http://relate.racai.ro:5000">RELATE</a>, the
 *         TEPROLIN web service.
 *         </p>
 */
public class RoTextProcessor extends TextProcessor {
	private static final String TEPROLIN_QUERY = "http://relate.racai.ro:5000/process";
	private static final Logger LOGGER = Logger.getLogger(RoTextProcessor.class.getName());
	private static final String UTF8_STRCONST = "UTF-8";
	private static final String CLITIC_QUERY = "https://relate.racai.ro/ws/cratima/asr_cratima.php";
	//private static final String UNKWORD_QUERY =
	//		"https://relate.racai.ro/ws/cratima/asr_correct.php";

	public RoTextProcessor(Lexicon lex, WordNet wn, RDSayings say) {
		super(lex, wn, say);
	}

	private String improveASRDetection(String text, String queryUrl) {
		StringBuilder content = new StringBuilder();

		try {
			URL url = new URL(queryUrl);
			URLConnection conn = url.openConnection();
			HttpURLConnection http = (HttpURLConnection) conn;

			http.setRequestMethod("GET");
			http.setDoOutput(true);

			Map<String, String> arguments = new HashMap<>();

			arguments.put("text", text);

			StringJoiner sj = new StringJoiner("&");

			for (Map.Entry<String, String> entry : arguments.entrySet()) {
				sj.add(URLEncoder.encode(entry.getKey(), UTF8_STRCONST) + "="
						+ URLEncoder.encode(entry.getValue(), UTF8_STRCONST));
			}

			byte[] out = sj.toString().getBytes(StandardCharsets.UTF_8);
			int length = out.length;

			http.setFixedLengthStreamingMode(length);
			http.setRequestProperty("Content-Type",
					"application/x-www-form-urlencoded; charset=UTF-8");
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
			} else {
				LOGGER.error("ASR improvement query error for text '" + text + "'; error code "
						+ status);
			}
		} catch (UnsupportedEncodingException uee) {
			uee.printStackTrace();
			return text;
		} catch (IOException ioe) {
			ioe.printStackTrace();
			return text;
		}

		String json = content.toString();
		JSONParser parser = new JSONParser();

		try {
			JSONObject root = (JSONObject) parser.parse(json);
			String textResult = (String) root.get("text");

			textResult = textResult.trim();

			if (!textResult.equalsIgnoreCase(text)) {
				text = textResult;
			}
		} catch (ParseException pe) {
			pe.printStackTrace();
		}

		return text;
	}

	/*
	 * (non-Javadoc)
	 * 
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

			Map<String, String> arguments = new HashMap<>();

			arguments.put("text", text);
			arguments.put("exec", "dependency-parsing");
			// It seems that UD-Pipe is not very good with some parses
			// that are needed by ROBIN.
			//arguments.put("dependency-parsing", "nlp-cube-adobe");

			StringJoiner sj = new StringJoiner("&");

			for (Map.Entry<String, String> entry : arguments.entrySet()) {
				sj.add(URLEncoder.encode(entry.getKey(), UTF8_STRCONST) + "="
						+ URLEncoder.encode(entry.getValue(), UTF8_STRCONST));
			}

			byte[] out = sj.toString().getBytes(StandardCharsets.UTF_8);
			int length = out.length;

			http.setFixedLengthStreamingMode(length);
			http.setRequestProperty("Content-Type",
					"application/x-www-form-urlencoded; charset=UTF-8");
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
			} else {
				LOGGER.error("TEPROLIN query error for text '" + text + "'; error code " + status);
			}
		} catch (UnsupportedEncodingException uee) {
			uee.printStackTrace();
			return new ArrayList<>();
		} catch (IOException ioe) {
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
		} catch (ParseException pe) {
			pe.printStackTrace();
		}

		return tokens;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see ro.racai.robin.nlp.TextProcessor#textCorrection(java.lang.String)
	 */
	@Override
	protected String textCorrection(String text) {
		if (!StringUtils.isNullEmptyOrBlank(text)) {
			// 1. Take care of clitic insertion (done by Vasile Păiș)
			text = improveASRDetection(text, CLITIC_QUERY);
			// 1.1 And further unknown word correction (also done by Vasile Păiș)
			//text = improveASRDetection(text, UNKWORD_QUERY);

			// 2. Replace known ASR errors with the correct Romanian phrases.
			List<String> tokens = Arrays.asList(text.split("\\s+"));
			Map<Pair<Integer, Integer>, String> corrected = new HashMap<>();

			if (!asrCorrectionDictionary.isEmpty()) {
				for (int k = asrMaxPhraseLength; k >= 1; k--) {
					for (int i = 0; i <= tokens.size() - k; i++) {
						Pair<Integer, Integer> rk = new Pair<>(i, i + k);
						boolean overlap = false;

						// Check if range i, i + k does not overlap with
						// an already recognized range
						for (Map.Entry<Pair<Integer, Integer>, String> e : corrected.entrySet()) {
							Pair<Integer, Integer> p = e.getKey();

							if ((rk.getFirstMember() >= p.getFirstMember()
									&& rk.getFirstMember() <= p.getSecondMember())
									|| (rk.getSecondMember() >= p.getFirstMember()
											&& rk.getSecondMember() <= p.getSecondMember())) {
								overlap = true;
								break;
							}
						}

						if (!overlap) {
							String phr = String.join(" ", tokens.subList(i, i + k)).toLowerCase();

							if (asrCorrectionDictionary.containsKey(phr)) {
								corrected.put(rk, asrCorrectionDictionary.get(phr));
								break;
							}
						}
					} // end for i
				} // end for k

				for (Map.Entry<Pair<Integer, Integer>, String> e : corrected.entrySet()) {
					Pair<Integer, Integer> p = e.getKey();

					tokens.set(p.getFirstMember(), e.getValue());

					for (int i = p.getFirstMember() + 1; i < p.getSecondMember(); i++) {
						tokens.set(i, "");
					}
				}

				text = String.join(" ", tokens);
				text = text.replaceAll("\\s+", " ");
				text = text.trim();
			}

			// 3. Add '?' or '.' depending on the statement.
			String[] spaceTokens = text.split("\\s+");

			if (spaceTokens.length == 1) {
				text += ".";
			} else if (lexicon.isQuestionFirstWord(spaceTokens[0])
					|| lexicon.isQuestionFirstWord(spaceTokens[1])) {
				text += "?";
			} else {
				text += ".";
			}

			// 4. Make first letter upper case.
			text = text.substring(0, 1).toUpperCase() + text.substring(1);
		}

		return text;
	}


	/**
	 * Edit the Romanian dependency parsing to make the copulative verb the root
	 * of the sentence.
	 * @param query the query to edit.
	 */
	private void editRootCopNSubjTriple(List<Token> query) {
		// Find root
		int rootIndex = -1;

		for (int i = 0; i < query.size(); i++) {
			Token t = query.get(i);

			if (t.drel.equals("root")) {
				rootIndex = i;

				if (t.pos.startsWith("V")) {
					// Nothing to edit. Bail out.
					return;
				} else {
					break;
				}
			}
		}

		// Find nsubj and cop
		int copIndex = -1;
		int nsubjIndex = -1;

		for (int i = 0; i < query.size(); i++) {
			Token t = query.get(i);

			if (t.head == rootIndex + 1) {
				if (t.drel.equals("nsubj")) {
					nsubjIndex = i;
				} else if (t.drel.equals("cop")) {
					copIndex = i;
				}
			}
		}

		if (copIndex >= 0 && nsubjIndex >= 0) {
			// If found:
			query.get(copIndex).drel = "root";
			query.get(copIndex).head = 0;

			if (query.get(copIndex).pos.length() > 2) {
				query.get(copIndex).pos = query.get(copIndex).pos.substring(0, 1) + "m"
						+ query.get(copIndex).pos.substring(2);
			} else {
				query.get(copIndex).pos = "Vm";
			}

			query.get(nsubjIndex).head = copIndex + 1;
			query.get(rootIndex).head = copIndex + 1;
			query.get(rootIndex).drel = "cop";

			// Redo root for all other tokens
			for (int i = 0; i < query.size(); i++) {
				Token t = query.get(i);

				if (t.head == rootIndex + 1) {
					// cop is the new root
					t.head = copIndex + 1;
				}
			}
		}
	}

	private List<Argument> editCatPredicate(List<Argument> arguments) {
		// Cât e ceasul?
		// Here we actually need a single argument, i.e. ceasul.
		// Delete "Cât".
		List<Argument> result = new ArrayList<>();

		if (arguments.size() == 2) {
			if (arguments.get(0).argTokens.size() == 1
					&& arguments.get(0).argTokens.get(0).lemma.equalsIgnoreCase("cât")) {
				result.add(arguments.get(1));
				result.get(0).isQueryVariable = true;
				return result;
			} else if (arguments.get(1).argTokens.size() == 1
					&& arguments.get(1).argTokens.get(0).lemma.equalsIgnoreCase("cât")) {
				result.add(arguments.get(0));
				result.get(0).isQueryVariable = true;
				return result;
			}
		}

		return arguments;
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see ro.racai.robin.nlp.TextProcessor#queryAnalyzer(java.util.List)
	 */
	@Override
	public Query queryAnalyzer(List<Token> query, List<RDConcept> concepts) {
		if (query == null) {
			return null;
		}

		// 1. Do query edits.
		editRootCopNSubjTriple(query);
		// Add more here if needed...

		Query result = new Query();
		int actionVerbID = 0;
		List<String> queryWords = query.stream().map(x -> x.wform).collect(Collectors.toList());

		// 2. If hello, return quickly.
		if (sayings.userOpeningStatement(queryWords)) {
			result.queryType = QType.HELLO;
			return result;
		}

		// 3. If goodbye, return quickly.
		if (sayings.userClosingStatement(queryWords)) {
			result.queryType = QType.GOODBYE;
			return result;
		}

		// 4. Find the root of the sentence. This has to be a main verb.
		for (int i = 0; i < query.size(); i++) {
			Token t = query.get(i);

			if (t.head == 0 && t.pos.startsWith("Vm")) {
				result.actionVerb = t.lemma.toLowerCase();
				// These are 1-based.
				actionVerbID = i + 1;
				break;
			}
		}

		if (actionVerbID == 0) {
			LOGGER.error(
					"Could not find an action verb in the query '" + queryToString(query) + "'");
			return null;
		}

		// We only have one query variable set.
		boolean queryVariableFlag = false;

		// 5. Find all arguments (first dependents) of the action verb.
		// We only consider "noun" arguments, e.g. nouns, pronouns, abbreviations, numerals, etc.
		for (int i = 0; i < query.size(); i++) {
			Token t = query.get(i);

			if (t.head == actionVerbID && lexicon.isNounPOS(t.pos) && !t.drel.equals("punct")) {
				t.isActionVerbDependent = true;

				List<Integer> belowIndexes = new ArrayList<>();
				List<Integer> nounPhraseIndexes = new ArrayList<>();

				belowIndexes.add(i + 1);
				treeUnder(query, belowIndexes, nounPhraseIndexes);
				nounPhraseIndexes.sort((Integer o1, Integer o2) -> o1.compareTo(o2));

				// -1 because all indexes are +1 to match
				// dependency parsing 1-based indexes
				List<Token> nounPhrase = nounPhraseIndexes.stream()
						// .filter((x) -> !lexicon.isFunctionalPOS(query.get(x - 1).POS))
						.map(x -> query.get(x - 1)).collect(Collectors.toList());

				boolean qvf = isQueryVariable(nounPhrase);

				if (!queryVariableFlag) {
					result.predicateArguments.add(new Argument(nounPhrase, qvf));

					if (qvf) {
						queryVariableFlag = true;
					}
				} else {
					result.predicateArguments.add(new Argument(nounPhrase, false));
				}
			}
		}
		
		// 6. Edit predicate arguments, if necessary.
		result.predicateArguments = editCatPredicate(result.predicateArguments);

		int fti = 0;

		// 8.1 Skip non-interesting words at the beginning of the user's sentence.
		while (fti < query.size() && lexicon.isSkippablePOS(query.get(fti).pos)) {
			fti++;
		}

		if (fti >= query.size() - 1) {
			return null;
		}

		Token firstToken = query.get(fti);
		Token secondToken = query.get(fti + 1);

		if (firstToken.lemma.equals("cât") && firstToken.pos.startsWith("R")) {
			// The "second" token here is the subject of the sentence.
			// Cât e ceasul? Cât e ora?
			for (Token t : query) {
				if (t.drel.equals("nsubj")) {
					secondToken = t;
					break;
				}
			}
		}

		// 8.2 Determine the query type.
		if (lexicon.isCommandVerb(result.actionVerb)) {
			result.queryType = QType.COMMAND;
		} else if (firstToken.lemma.equals("cine")) {
			result.queryType = QType.PERSON;
		} else if (firstToken.lemma.equals("ce") || firstToken.lemma.equals("cât")
				|| firstToken.lemma.equals("care")) {
			// Default that's a WHAT type question.
			// It may be specialized on PERSON, LOCATION or TIME, otherwise it's WORD.
			result.queryType = QType.WHAT;

			if (lexicon.isPureNounPOS(secondToken.pos) && concepts != null) {
				for (RDConcept c : concepts) {
					if (c.isThisConcept(secondToken.lemma, wordNet) && c.getType() != CType.WORD) {
						boolean wasSet = false;

						switch (c.getType()) {
							case PERSON:
								result.queryType = QType.PERSON;
								wasSet = true;
								break;
							case LOCATION:
								result.queryType = QType.LOCATION;
								wasSet = true;
								break;
							case TIME:
								result.queryType = QType.TIME;
								wasSet = true;
								break;
							default:
						}

						if (wasSet) {
							break;
						}
					}
				} // end all concepts
			}
		} else if (firstToken.lemma.equals("unde")) {
			result.queryType = QType.LOCATION;
		} else if (firstToken.lemma.equals("când")) {
			result.queryType = QType.TIME;
		} else if (firstToken.lemma.equals("cum")) {
			result.queryType = QType.HOW;
		} else {
			result.queryType = QType.YESNO;
		}

		return result;
	}

	/**
	 * Extracts the portion of the sentence under the dependency tree rooted at index.
	 * 
	 * @param query       the list of tokens to use
	 * @param checkHeads  list of heads to search for in the dependency tree; initially it only
	 *                    contains the root index
	 * @param storedHeads list of heads that are "below" root index
	 * @return a list of integers for indexes that are "below" the starting index
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
		if (argument != null && !argument.isEmpty()) {
			int firstIndex = 0;

			if (argument.get(0).pos.startsWith("S")) {
				// Remove first preposition, if it exists.
				firstIndex = 1;
			}

			// Relative pronoun/determiner/adverb
			if ((argument.get(firstIndex).pos.length() >= 2
					&& argument.get(firstIndex).pos.charAt(1) == 'w')
					|| lexicon.isQuestionFirstWord(argument.get(firstIndex).wform)) {
				return true;
			}
		}

		return false;
	}

	@Override
	public String expandEntities(String text) {
		text = text.trim();

		// 0. Text may be an instantiation of a response generator
		if (text.startsWith("ro.racai.robin.dialog.generators.")) {
			try {
				Class<?> clazz = Class.forName(text);
				RDResponseGenerator generator =
						(RDResponseGenerator) clazz.getDeclaredConstructor().newInstance();

				text = generator.generate();
			} catch (ClassNotFoundException | NoSuchMethodException | InstantiationException
					| IllegalAccessException | InvocationTargetException e) {
				e.printStackTrace();
				return "Eroare de inițializare a generatorului de răspuns.";
			}
		}

		Map<Integer, Pair<EntityType, Integer>> entities = lexicon.markEntities(text);
		Map<Integer, Pair<Integer, String>> replacements = new HashMap<>();

		// 1. Expand all found entities
		for (Map.Entry<Integer, Pair<EntityType, Integer>> e : entities.entrySet()) {
			int offset = e.getKey();
			Pair<EntityType, Integer> pair = e.getValue();
			EntityType eType = pair.getFirstMember();
			int length = pair.getSecondMember();
			String entityAtOffset = text.substring(offset, offset + length);
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
					break;
				case MODEL:
					eText = lexicon.sayModel(entityAtOffset);
					replacements.put(offset, new Pair<>(length, eText));
					break;
				default:
					break;
			}
		} // end all offsets

		// 2. Insert the replacements back into the original text
		List<Integer> allOffsets = new ArrayList<>();

		for (int offset : entities.keySet()) {
			if (allOffsets.isEmpty()) {
				allOffsets.add(offset);
			} else {
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

		if (walkIndex < text.length()) {
			result.append(text.substring(walkIndex));
		}

		if (result.length() == 0) {
			return text;
		}

		return result.toString();
	}

	@Override
	public List<Token> postProcessing(List<Token> tokens) {
		// Do POS tagging corrections
		for (Token t : tokens) {
			Pair<String, String> pl = lexicon.getPOSAndLemmaForWord(t.wform);

			if (pl != null) {
				t.pos = pl.getFirstMember();
				t.lemma = pl.getSecondMember();
			}
		}
		
		if (!tokens.isEmpty() && tokens.get(0).pos.startsWith("V")) {
			// This is a question in Romanian, usually.
			Token last = tokens.get(tokens.size() - 1);

			last.wform = "?";
			last.lemma = "?";
			last.pos = "QUEST";
		}

		return tokens;
	}
}
