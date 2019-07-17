/**
 * 
 */
package ro.racai.robin.nlp;

import java.util.List;

/**
 * @author Radu Ion ({@code radu@racai.ro})
 * <p>An interface to a WordNet-like semantic network.
 * Currently used to check if retrieve words from different
 * semantic relations.</p>
 */
public interface WordNet {
	/**
	 * <p>Get a list of hypernyms a given {@code word}.</p>
	 * regardless of their senses.</p>
	 * @param word   the word to get hypernyms for;
	 * @return       {@link java.util.List} with the hypernyms of word,
	 *               regardless of the meaning.
	 */
	public List<String> getHypernyms(String word);

	/**
	 * <p>Get a list of hyponyms a given {@code word}.</p>
	 * regardless of their senses.</p>
	 * @param word   the word to get hyponyms for;
	 * @return       {@link java.util.List} with the hyponyms of word,
	 *               regardless of the meaning.
	 */
	public List<String> getHyponyms(String word);

	/**
	 * <p>Get the list of synonyms for a given {@code word}.</p>
	 * @param word   the word to get synonyms for;
	 * @return       {@link java.util.List} with the synonyms of w,
	 *               regardless of the meaning.
	 */
	public List<String> getSynonyms(String word);
}
