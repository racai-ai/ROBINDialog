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
	 * This is the list of instantiated concepts constructed
	 * during the creation of the micro-world. The text processor
	 * must know about them to properly set the query type.
	 * If {@code null}, it will NOT be used.
	 */
	protected List<RDConcept> universeConcepts;
	
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
	private final String processedTextCacheFile =
		"processed-text-cache.txt";
	protected Map<String, List<Token>> processedTextCache =
		new HashMap<String, List<Token>>();
	
	/**
	 * @author Radu Ion ({@code radu@racai.ro})
	 * <p>Represents an annotated token of the input text.
	 * The member field names are self explanatory.</p>
	 */
	public static class Token {
		public String wform;
		public String lemma;
		public String POS;
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
			POS = p;
			head = h;
			drel = dr;
			isActionVerbDependent = avd;
		}

		public String textRecord() {
			return wform + "\t" + lemma + "\t" + POS + "\t" + drel + "\t" + head + "\t" + isActionVerbDependent;
		}
		
		/* (non-Javadoc)
		 * @see java.lang.Object#toString()
		 */
		@Override
		public String toString() {
			return wform + "/" + lemma + "/" + POS + " " + drel + "<-" + head;
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
		public List<Argument> predicateArguments = new ArrayList<Argument>();
	}
	
	public TextProcessor(Lexicon lex, WordNet wn, RDSayings say) {
		lexicon = lex;
		sayings = say;
		populateProcessedTextCache();
	}
	
	/**
	 * <p>Give it a text (from the ASR engine) and get back
	 * a list of {@link Token}s that are annotated.</p> 
	 * @param text        the text to be analyzed
	 * @return            the list of tokens to work with
	 */
	public List<Token> textProcessor(String text) {
		text = normalizeText(text);
		
		if (processedTextCache.containsKey(text)) {
			return processedTextCache.get(text);
		}
		
		List<Token> procText = processText(text);
		
		processedTextCache.put(text, procText);
		
		return procText;
	}
	
	/**
	 * <p>Returns the length of a sentence disregarding
	 * functional words.</p>
	 * @param sentence       the sentence to compute length
	 *                       for;
	 * @return               the number of content words
	 *                       in the sentence.
	 */
	public int noFunctionalWordsLength(List<Token> sentence) {
		int len = 0;
		
		if (sentence == null) {
			return len;
		}
		
		for (Token t : sentence) {
			if (!lexicon.isFunctionalPOS(t.POS)) {
				len++;
			}
		}
		
		return len;
	}
	
	private void populateProcessedTextCache() {
		if (!new File(processedTextCacheFile).exists()) {
			// On first run this file does not exist yet.
			return;
		}
		
		try {
			BufferedReader rdr =
				new BufferedReader(
					new InputStreamReader(
						new FileInputStream(processedTextCacheFile), "UTF8"));
			String line = rdr.readLine();
			
			while (line != null) {
				String text = line;
				List<Token> textProc = new ArrayList<Token>();
				
				line = rdr.readLine();
				
				while (!line.isEmpty()) {
					String[] parts = line.split("\\s+");
					String wform = parts[0];
					String lemma = parts[1];
					String POS = parts[2];
					String drel = parts[3];
					int head = Integer.parseInt(parts[4]);
					boolean avd = Boolean.parseBoolean(parts[5]);
					
					textProc.add(new Token(wform, lemma, POS, head, drel, avd));
					line = rdr.readLine();
				}
				
				processedTextCache.put(text, textProc);
				line = rdr.readLine();
			}
			
			rdr.close();
		}
		catch (IOException ioe) {
			LOGGER.warn("Could not open or read " + processedTextCacheFile);
			ioe.printStackTrace();
		}
	}

	public void dumpTextCache() {
		try {
			BufferedWriter wrt =
				new BufferedWriter(
					new OutputStreamWriter(
						new FileOutputStream(processedTextCacheFile), "UTF8"));
			
			for (String text : processedTextCache.keySet()) {
				wrt.write(text);
				wrt.newLine();
				
				for (Token tok : processedTextCache.get(text)) {
					wrt.write(tok.textRecord());
					wrt.newLine();
				}
				
				wrt.newLine();
			}
			
			wrt.close();
		}
		catch (IOException ioe) {
			LOGGER.warn("Could not open or write to " + processedTextCacheFile);
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
	public abstract String textCorrection(String text);
	
	/**
	 * <p>When saying e.g. '245', we need to transform
	 * the number into a sequence of words. Also, when saying
	 * English acronyms, e.g. 'IBM', we need a phonetic transcription
	 * in Romanian, e.g. 'aibiem'.
	 */
	public abstract String expandEntities(String text);

	/**
	 * <p>Main method of query analysis. This method will
	 * construct a "parse" of the text query received,
	 * in the instance of a {@link Query} object.</p>
	 * @param query      the text query to be mined for the
	 *                   action verb and its arguments.
	 * @return           the {@link Query} object or {@code null}
	 *                   if something went wrong.
	 */
	public abstract Query queryAnalyzer(List<Token> query);
	
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
	
	/**
	 * <p>Used to the the concept list to be used in this processor.</p>
	 * @param conList        the concept list to be set.
	 */
	public void setConceptList(List<RDConcept> conList) {
		universeConcepts = conList;
	}
}
