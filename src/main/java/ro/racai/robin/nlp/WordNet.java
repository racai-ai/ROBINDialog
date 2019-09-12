/**
 * 
 */
package ro.racai.robin.nlp;

import java.util.List;

/**
 * @author Radu Ion ({@code radu@racai.ro})
 * <p>An interface to a WordNet-like semantic network.
 * Currently used to retrieve words that form different
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
	
	/**
	 * <p>Does a WordNet first order neighborhood search to
	 * see if the two parameters can be made equal.</p> 
	 * @param w1         first word parameter
	 * @param w2         second word parameter
	 * @return           {@code true} if {@code w1} and {@code w2}
	 *                   are synonyms, first order hyponyms/hypernyms
	 */
	public boolean wordnetEquals(String w1, String w2);
}
