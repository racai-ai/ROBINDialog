package ro.racai.robin.nlp;

import java.util.Map;
import ro.racai.robin.dialog.CType;

/**
 * @author Radu Ion ({@code radu@racai.ro})
 *         <p>
 *         An interface to build a "verb" inventory with meanings given by the method name.
 *         Implement this for your language. Also add other meaning-related methods or word-related
 *         methods.
 *         </p>
 */
public interface Lexicon {
	/**
	 * For words that the POS tagger gets it wrong, get the right tag from here.
	 * @param word the word to get the tag for.
	 * @return
	 */
	public default Pair<String, String> getPOSAndLemmaForWord(String word) {
		return new Pair<>("X", word);
	}

	public default boolean isSubjectOrDirectObject(String drel) {
		return drel.startsWith("nsubj") || drel.equals("obj");
	}

	/**
	 * Checks a verb lemma for a "command" verb.
	 * 
	 * @param verbLemma the verb lemma to check
	 * @return {@code true} if lemma is a command verb
	 */
	public boolean isCommandVerb(String verbLemma);

	/**
	 * Checks a verb lemma for verb that asks for an {@link CType#AMOUNT}, e.g. "costa".
	 * 
	 * @param verbLemma the verb lemma to check
	 * @return {@code true} if lemma is an amount verb
	 */
	public boolean isAmountVerb(String verbLemma);

	public boolean isAmountVariableWord(String lemma);

	/**
	 * Checks if the POS belongs to a functional word.
	 * 
	 * @param pos the POS to check
	 * @return {@code true} if {@code pos} belongs to a functional word.
	 */
	public boolean isFunctionalPOS(String pos);

	/**
	 * Checks to see if {@code word} is a functional word.
	 * 
	 * @param word the word to be checked.
	 * @return {@code true} if {@code word} is a functional word.
	 */
	public boolean isFunctionalWord(String word);

	/**
	 * Checks if POS is a noun POS, with friends such as pronouns.
	 * 
	 * @param pos the POS to check
	 * @return {@code true} if {@code pos} is a noun
	 */
	public boolean isNounPOS(String pos);

	/**
	 * Checks if POS is a noun POS, only.
	 * 
	 * @param pos the POS to check
	 * @return {@code true} if {@code pos} is a noun
	 */
	public boolean isPureNounPOS(String pos);

	/**
	 * Checks if POS can be skipped at the beginning of the sentence.
	 * 
	 * @param pos the POS to check
	 * @return {@code true} if {@code pos} is a preposition
	 */
	public boolean isSkippablePOS(String pos);

	/**
	 * Checks this word to see if it can start a question.
	 * 
	 * @param word the word to check
	 * @return {@code true} if this word can start a question
	 */
	public boolean isQuestionFirstWord(String word);

	/**
	 * Will take a number and transform it to its literal equivalent, e.g. 249 -> 'două sute
	 * patruzeci și nouă'
	 * 
	 * @param number the number to transform
	 * @return the language specific representation of the given number
	 */
	public String sayNumber(String number);

	/**
	 * Will take a time specification and transform it to its literal equivalent, e.g. 8:30 -> 'ora
	 * opt și treizeci de minute'
	 * 
	 * @param time the time to transform
	 * @return the language specific representation of the given time
	 */
	public String sayTime(String time);

	/**
	 * Will take a date specification and transform it to its literal equivalent, e.g. 28/05/2020 ->
	 * 'douăzeci și opt mai două mii douăzeci'
	 * 
	 * @param date the date to transform
	 * @return the language specific representation of the given date
	 */
	public String sayDate(String date);

	/**
	 * Will take a model name, e.g. 'AG45-12' and will transcribe it as 'a ge patruzeci și cinci
	 * doisprezece'
	 * 
	 * @param model the model to transform
	 * @return the language specific representation of the given model
	 */
	public String sayModel(String model);

	/**
	 * Find and mark, returning a Map from offsets to a {@link Pair} of entity type and entity
	 * length in text.
	 * 
	 * @param text the text to scan for know entity types.
	 */
	public Map<Integer, Pair<EntityType, Integer>> markEntities(String text);
}
