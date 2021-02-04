/**
 * 
 */
package ro.racai.robin.nlp;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.log4j.Logger;

import ro.racai.robin.dialog.RDConcept;
import ro.racai.robin.dialog.RDSayings;

/**
 * @author Radu Ion ({@code radu@racai.ro})
 * <p>This class will take a bare text String and it will
 * add POS tagging, lemmatization and dependency parsing.</p>
 */
public abstract class TextProcessor {
	private static final Logger LOGGER =
		Logger.getLogger(TextProcessor.class.getName());
	
	/**
	 * Lexicon to use for text processing. 
	 */
	protected Lexicon lexicon;
	
	/**
	 * WordNet to use in text processing. 
	 */
	protected WordNet wordNet;
	
	/**
	 * Fixed expressions to be recognized.
	 */
	protected RDSayings sayings;
	
	/**
	 * Save expensive text processing calls
	 * to the TEPROLIN web service. 
	 */
	protected static final String PROCESSED_TEXT_CACHE_FILE = "processed-text-cache.txt";
	protected Map<String, List<Token>> processedTextCache = new HashMap<>();
	
	/**
	 * The correction dictionary for the ASR module.
	 */
	protected Map<String, String> asrCorrectionDictionary = new HashMap<>();
	protected int asrMaxPhraseLength = 0;
	
	/**
	 * @author Radu Ion ({@code radu@racai.ro})
	 * <p>Represents an annotated token of the input text.
	 * The member field names are self explanatory.</p>
	 */
	public static class Token {
		public String wform;
		public String lemma;
		public String pos;
		// Head of this token in the
		// dependency tree.
		public int head;
		// The name of the dependency relation
		// that holds between this token and its
		// head.
		public String drel;
		// True if this token is directly linked
		// to the action verb of the query.
		public boolean isActionVerbDependent;
		
		public Token(String w, String l, String p, int h, String dr, boolean avd) {
			wform = w;
			lemma = l;
			pos = p;
			head = h;
			drel = dr;
			isActionVerbDependent = avd;
		}

		public String textRecord() {
			return wform + "\t" + lemma + "\t" + pos + "\t" + drel + "\t" + head + "\t" + isActionVerbDependent;
		}
		
		/* (non-Javadoc)
		 * @see java.lang.Object#toString()
		 */
		@Override
		public String toString() {
			return wform + "/" + lemma + "/" + pos + " " + drel + "<-" + head;
		}
	}

	/**
	 * @author Radu Ion ({@code radu@racai.ro})
	 * <p>This is the ``argument'' of a predicate, as
	 * seen in the syntactic parsing of the sentence.</p>
	 */
	public static class Argument {
		/**
		 * These are the argument tokens
		 * as uttered by the user. 
		 */
		public List<Token> argTokens;
		
		/**
		 * <p>{@code true} if this argument represents
		 * the missing information that the user requires.</p>
		 * <p>For example:</p>
		 * <p><i>În ce sală se desfășoară cursul de informatică?</i></p>
		 * <p>Here, ``În ce sală'' is the query variable.</p>  
		 */
		public boolean isQueryVariable;
		
		public Argument(List<Token> toks, boolean isvar) {
			argTokens = toks;
			isQueryVariable = isvar;
		}
	}
	
	/**
	 * @author Radu Ion ({@code radu@racai.ro})
	 * <p>This is the query object that has been extracted
	 * from the user's request in written Romanian.</p>
	 */
	public static class Query {
		/**
		 * What the query asks for,
		 * e.g. a person, a location, etc. 
		 */
		public QType queryType;
		
		/**
		 * What is the main verb (lemma) of the query/question.
		 * It can be a command-type verb, e.g. "duce", "conduce",
		 * "arată", etc. or some informative verb such as "costa",
		 * "fi", "afla", etc.
		 */
		public String actionVerb;

		/**
		 * Arguments of the {@link #actionVerb}.
		 * An instantiation of an {@link RDConcept} -- to be matched
		 * against a concept, e.g. "laboratorul de robotică".
		 */
		public List<Argument> predicateArguments = new ArrayList<>();

		/**
		 * Checks for a factual question, e.g. Cine/Ce/Când/Cât ...
		 * @return {@code true} if query has an unbound variable to be resolved.
		 */
		public boolean hasQueryVariable() {
			for (Argument a : predicateArguments) {
				if (a.isQueryVariable) {
					return true;
				}
			}

			return false;
		}
	}
	
	protected TextProcessor(Lexicon lex, WordNet wn, RDSayings say) {
		lexicon = lex;
		sayings = say;
		wordNet = wn;
		populateProcessedTextCache();
	}
	
	public void setASRDictionary(Map<String, String> dictionary) {
		asrCorrectionDictionary = dictionary;

		for (Map.Entry<String, String> e : asrCorrectionDictionary.entrySet()) {
			String[] parts = e.getKey().split("\\s+");

			if (asrMaxPhraseLength < parts.length) {
				// Establish the maximum length of an erroneous phrase
				asrMaxPhraseLength = parts.length;
			}
		}
	}

	/**
	 * <p>
	 * Give it a text (from the ASR engine or from the .mw file) and get back a list of
	 * {@link Token}s that are annotated.
	 * </p>
	 * 
	 * @param text the text to be analyzed
	 * @param isJavaRef if {@code true}, special, one-token processing is performed.
	 * @param isFromMW   if {@code true}, no text normalization and correction is performed.
	 * @return the list of tokens to work with.
	 */
	public List<Token> textProcessor(String text, boolean isJavaRef, boolean isFromMW) {
		if (isJavaRef) {
			// This is a Java class name
			Token t = new Token(text, text, "Nc", 0, "root", false);
			List<Token> procText = new ArrayList<>();

			procText.add(t);
			return procText;
		}

		if (!isFromMW) {
			// This is a piece of text coming from the .mw file
			text = normalizeText(text);
			text = textCorrection(text);
		}

		if (processedTextCache.containsKey(text)) {
			return processedTextCache.get(text);
		}

		List<Token> procText = processText(text);

		procText = postProcessing(procText);
		processedTextCache.put(text, procText);

		return procText;
	}
	
	/**
	 * Call this to pretty-print the prompt to the user.
	 * 
	 * @param tokens the list of tokens to print.
	 * @return prompt {@link String}.
	 */
	public String toPromptString(List<Token> tokens) {
		if (tokens.isEmpty()) {
			return "";
		}

		StringBuilder result = new StringBuilder();

		result.append(tokens.get(0).wform);

		for (int i = 1; i < tokens.size(); i++) {
			Token t = tokens.get(i);

			if (t.wform.matches("^\\W+$")) {
				result.append(t.wform);
			}
			else {
				result.append(" " + t.wform);
			}
		}

		return result.toString();
	}

	/**
	 * If more post-processing of the text is needed, put it into this method.
	 * By default, it does nothing.
	 * @param tokens the list of tokens to modify.
	 * @return the modified token list.
	 */
	protected List<Token> postProcessing(List<Token> tokens) {
		return tokens;
	}
	
	/**
	 * <p>
	 * Returns a version of the {@code sentence} disregarding functional words.
	 * </p>
	 * 
	 * @param sentence the sentence to filter;
	 * @return the filtered sentence.
	 */
	public List<Token> noFunctionalWordsFilter(List<Token> sentence) {
		if (sentence == null || sentence.isEmpty()) {
			return sentence;
		}

		List<Token> result = new ArrayList<>();

		for (Token t : sentence) {
			if (!lexicon.isFunctionalPOS(t.pos)) {
				result.add(t);
			}
		}

		return result;
	}
	
	private void populateProcessedTextCache() {
		if (!new File(PROCESSED_TEXT_CACHE_FILE).exists()) {
			// On first run this file does not exist yet.
			return;
		}
		
		try (BufferedReader rdr = new BufferedReader(new InputStreamReader(
				new FileInputStream(PROCESSED_TEXT_CACHE_FILE), StandardCharsets.UTF_8))) {
			String line = rdr.readLine();
			
			while (line != null) {
				String text = line;
				List<Token> textProc = new ArrayList<>();
				
				line = rdr.readLine();
				
				while (!line.isEmpty()) {
					String[] parts = line.split("\\s+");
					String wform = parts[0];
					String lemma = parts[1];
					String pos = parts[2];
					String drel = parts[3];
					int head = Integer.parseInt(parts[4]);
					boolean avd = Boolean.parseBoolean(parts[5]);
					
					textProc.add(new Token(wform, lemma, pos, head, drel, avd));
					line = rdr.readLine();
				}
				
				processedTextCache.put(text, textProc);
				line = rdr.readLine();
			}
		}
		catch (IOException ioe) {
			LOGGER.warn("Could not open or read " + PROCESSED_TEXT_CACHE_FILE);
			ioe.printStackTrace();
		}
	}

	public void dumpTextCache() {
		try (BufferedWriter wrt = new BufferedWriter(new OutputStreamWriter(
				new FileOutputStream(PROCESSED_TEXT_CACHE_FILE), StandardCharsets.UTF_8))) {
			
			for (Map.Entry<String, List<Token>> e : processedTextCache.entrySet()) {
				wrt.write(e.getKey());
				wrt.newLine();
				
				for (Token tok : e.getValue()) {
					wrt.write(tok.textRecord());
					wrt.newLine();
				}
				
				wrt.newLine();
			}
		}
		catch (IOException ioe) {
			LOGGER.warn("Could not open or write to " + PROCESSED_TEXT_CACHE_FILE);
			ioe.printStackTrace();
		}
	}

	/**
	 * <p>Implement this to get the annotations inside
	 * a {@link Token}.</p>
	 * @param text      text to be processed
	 * @return          the list of tokens
	 */
	protected abstract List<Token> processText(String text);
	
	/**
	 * <p>If the text comes from an ASR engine, it may have
	 * errors, so use this method to correct it, if possible.</p>
	 * @param text          text to be corrected
	 * @return              the fixed text
	 */
	protected abstract String textCorrection(String text);
	
	/**
	 * <p>When saying e.g. '245', we need to transform
	 * the number into a sequence of words. Also, when saying
	 * English acronyms, e.g. 'IBM', we need a phonetic transcription
	 * in Romanian, e.g. 'aibiem'.
	 */
	public abstract String expandEntities(String text);

	/**
	 * <p>
	 * Main method of query analysis. This method will construct a "parse" of the text query
	 * received, in the instance of a {@link Query} object.
	 * </p>
	 * 
	 * @param query    the text query to be mined for the action verb and its arguments.
	 * @param concepts the defined concepts in the universe of discourse (not the bound ones).
	 * @return the {@link Query} object or {@code null} if something went wrong.
	 */
	public abstract Query queryAnalyzer(List<Token> query, List<RDConcept> concepts);
	
	/**
	 * <p>Optional call before calling {@link #processText(String)}.</p>
	 * @param text       the text to be normalized
	 * @return           normalized text
	 */
	protected String normalizeText(String text) {
		text = text.trim();
		text = text.replaceAll("\\s+", " ");
		
		return text;
	}
	
	/**
	 * <p>Checks to see if list of tokens could represent
	 * a ``variable'', e.g. ``cine'', ``unde'', ``ce sală'', etc.</p>
	 * @param argument      the list of tokens to check for a variable; 
	 * @return              {@code true} if this list of tokens represents a variable.
	 */
	public abstract boolean isQueryVariable(List<Token> argument);
}
