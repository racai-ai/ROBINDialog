/**
 * 
 */
package ro.racai.robin.nlp;

import java.util.ArrayList;
import java.util.List;

import ro.racai.robin.dialog.RDConcept;

/**
 * @author Radu Ion ({@code radu@racai.ro})
 * <p>This class will take a bare text String and it will
 * add POS tagging, lemmatization and dependency parsing.</p>
 */
public abstract class TextProcessor {
	/**
	 * Lexicon to use for text processing. 
	 */
	protected Lexicon lexicon;
	
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
			isActionVerbDependent = true;
		}

		/* (non-Javadoc)
		 * @see java.lang.Object#toString()
		 */
		@Override
		public String toString() {
			return wform + "/" + lemma + "/" + POS + " <-" + drel + "- " + head;
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
		public List<List<Token>> predicateArguments = new ArrayList<List<Token>>();
	}
	
	public TextProcessor(Lexicon lex) {
		lexicon = lex;
	}
	
	/**
	 * <p>Give it a text (from the ASR engine) and get back
	 * a list of {@link Token}s that are annotated.</p> 
	 * @param text        the text to be analyzed
	 * @return            the list of tokens to work with
	 */
	public List<Token> textProcessor(String text) {
		text = normalizeText(text);
		text = textCorrection(text);
		
		return processText(text);
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
	 * errors, so use this method to correct it if possible.</p>
	 * @param text          text to be corrected
	 * @return              the fixed text
	 */
	protected abstract String textCorrection(String text);
	
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
		text = Character.toUpperCase(text.charAt(0)) + text.substring(1);
		
		return text;
	}
}
