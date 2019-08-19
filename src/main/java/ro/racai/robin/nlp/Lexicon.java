/**
 * 
 */
package ro.racai.robin.nlp;

import java.util.List;

/**
 * @author Radu Ion ({@code radu@racai.ro})
 * <p>An interface to build a "verb" inventory
 * with meanings given by the method name. Implement
 * this for your language. Also add other meaning-related
 * methods or word-related methods.</p>
 */
public interface Lexicon {
	/**
	 * Checks a verb lemma for a "command" verb.
	 * @param verbLemma     the verb lemma to check
	 * @return              {@code true} if lemma is a command verb
	 */
	public boolean isCommandVerb(String verbLemma);
	
	/**
	 * Checks if the POS belongs to a functional word.
	 * @param pos        the POS to check
	 * @return           {@code true} if {@code pos} belongs to a
	 *                   functional word.
	 */
	public boolean isFunctionalPOS(String pos);
	
	/**
	 * Checks if POS is a noun POS.
	 * @param pos        the POS to check
	 * @return           {@code true} if {@code pos} is a noun
	 */
	public boolean isNounPOS(String pos);
	
	/**
	 * Checks if POS is a preposition POS.
	 * @param pos        the POS to check
	 * @return           {@code true} if {@code pos} is a preposition
	 */
	public boolean isPrepositionPOS(String pos);
	
	/**
	 * User has ended the dialog with a "Thank you"
	 * "Goodbye".
	 * @param words      a list of words to check if
	 *                   they mark the end of the conversation (EOC)
	 * @return           {@code true} if words mark the EOC
	 */
	public boolean isClosingStatement(List<String> words);
}
