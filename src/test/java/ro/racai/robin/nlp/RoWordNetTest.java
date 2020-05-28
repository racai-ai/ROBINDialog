/**
 * 
 */
package ro.racai.robin.nlp;

import static org.junit.Assert.*;

import java.util.List;

import org.junit.Test;

/**
 * @author Radu Ion ({@code radu@racai.ro})
 * <p>Tests the Romanian WordNet implementation, based on
 * RELATE portal.</p>
 */
public class RoWordNetTest {

	@Test
	public void testCopacSyn() {
		RoWordNet rown = new RoWordNet();
		List<String> synonyms = rown.getSynonyms("copac");

		assertTrue(synonyms.get(0).equals("arbore"));
		assertTrue(synonyms.get(1).equals("pom"));
	}
	
	@Test
	public void testMasinaSyn() {
		RoWordNet rown = new RoWordNet();
		List<String> synonyms = rown.getSynonyms("mașină");

		assertTrue(synonyms.get(0).equals("mașină-unealtă"));
		assertTrue(synonyms.get(1).equals("automobil"));
		assertTrue(synonyms.get(2).equals("locomotivă"));
	}
	
	@Test
	public void testCopacHypo() {
		RoWordNet rown = new RoWordNet();
		List<String> hyponyms = rown.getHyponyms("copac");

		assertTrue(hyponyms.get(0).equals("piper_de_Guineea"));
		assertTrue(hyponyms.get(1).equals("anason"));
	}
	
	@Test
	public void testCopacHyper() {
		RoWordNet rown = new RoWordNet();
		List<String> hypernyms = rown.getHypernyms("copac");

		assertTrue(hypernyms.get(0).equals("plantă_lemnoasă"));
	}
}
