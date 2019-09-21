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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;


/**
 * @author Radu Ion ({@code radu@racai.ro})
 * <p>An interface to a WordNet-like semantic network.
 * Currently used to retrieve words that form different
 * semantic relations.</p>
 */
public abstract class WordNet {
	private static final Logger LOGGER =
		Logger.getLogger(WordNet.class.getName());
	
	/**
	 * The equals cache map, to avoid
	 * expensive calls to the RELATE platform. 
	 */
	protected Map<String, Boolean> wnEqualsCache;
	
	/**
	 * Where to save the WordNet equals cache. 
	 */
	protected String wnEqualsCacheFile = "wordnet-cache.txt";
	
	public WordNet() {
		wnEqualsCache = new HashMap<String, Boolean>();
		populateWordNetEqualsCache();
	}
	
	private void populateWordNetEqualsCache() {
		if (!new File(wnEqualsCacheFile).exists()) {
			// On first run this file does not exist yet.
			return;
		}
		
		try {
			BufferedReader rdr =
				new BufferedReader(
					new InputStreamReader(
						new FileInputStream(wnEqualsCacheFile), "UTF8"));
			String line = rdr.readLine();
			
			while (line != null) {
				String[] parts = line.split("\\s+");
				
				wnEqualsCache.put(parts[0], Boolean.parseBoolean(parts[1]));
				line = rdr.readLine();
			}
			
			rdr.close();
		}
		catch (IOException ioe) {
			LOGGER.warn("Could not open or read " + wnEqualsCacheFile);
			ioe.printStackTrace();
		}
	}

	public void dumpWordNetCache() {
		try {
			BufferedWriter wrt =
				new BufferedWriter(
					new OutputStreamWriter(
						new FileOutputStream(wnEqualsCacheFile), "UTF8"));
			
			for (String eqk : wnEqualsCache.keySet()) {
				wrt.write(eqk + "\t" + wnEqualsCache.get(eqk));
				wrt.newLine();
			}
			
			wrt.close();
		}
		catch (IOException ioe) {
			LOGGER.warn("Could not open or write to " + wnEqualsCacheFile);
			ioe.printStackTrace();
		}
	}
	
	/**
	 * <p>Get a list of hypernyms a given {@code word}.</p>
	 * regardless of their senses.</p>
	 * @param word   the word to get hypernyms for;
	 * @return       {@link java.util.List} with the hypernyms of word,
	 *               regardless of the meaning.
	 */
	public abstract List<String> getHypernyms(String word);

	/**
	 * <p>Get a list of hyponyms a given {@code word}.</p>
	 * regardless of their senses.</p>
	 * @param word   the word to get hyponyms for;
	 * @return       {@link java.util.List} with the hyponyms of word,
	 *               regardless of the meaning.
	 */
	public abstract List<String> getHyponyms(String word);

	/**
	 * <p>Get the list of synonyms for a given {@code word}.</p>
	 * @param word   the word to get synonyms for;
	 * @return       {@link java.util.List} with the synonyms of w,
	 *               regardless of the meaning.
	 */
	public abstract List<String> getSynonyms(String word);
	
	/**
	 * <p>Does a WordNet first order neighborhood search to
	 * see if the two parameters can be made equal.</p> 
	 * @param w1         first word parameter
	 * @param w2         second word parameter
	 * @return           {@code true} if {@code w1} and {@code w2}
	 *                   are synonyms, first order hyponyms/hypernyms
	 */
	public boolean wordnetEquals(String w1, String w2) {
		String key12 = w1 + "#" + w2;
		String key21 = w2 + "#" + w1;
		
		if (wnEqualsCache.containsKey(key12)) {
			return wnEqualsCache.get(key12);
		}

		if (wnEqualsCache.containsKey(key21)) {
			return wnEqualsCache.get(key21);
		}
		
		// Synonym check with WordNet
		for (String syn : getSynonyms(w1)) {
			if (w2.equals(syn)) {
				wnEqualsCache.put(key12, true);
				wnEqualsCache.put(key21, true);
				
				return true;
			}
		}
		
		// Use hypernyms from WordNet (only direct hypernyms)
		for (String hyper : getHypernyms(w1)) {
			if (w2.equals(hyper)) {
				wnEqualsCache.put(key12, true);
				wnEqualsCache.put(key21, true);
				
				return true;
			}
		}
		
		// Use hyponyms from WordNet (only direct hyponyms)
		for (String hypo : getHyponyms(w1)) {
			if (w2.equals(hypo)) {
				wnEqualsCache.put(key12, true);
				wnEqualsCache.put(key21, true);
				
				return true;
			}
		}
		
		wnEqualsCache.put(key12, false);
		wnEqualsCache.put(key21, false);
		
		return false;
	}
}
