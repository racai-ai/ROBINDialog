/**
 * 
 */
package ro.racai.robin.nlp;

import static org.junit.Assert.assertEquals;

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

		assertEquals("arbore", synonyms.get(0));
		assertEquals("pom", synonyms.get(1));
	}
	
	@Test
	public void testMasinaSyn() {
		RoWordNet rown = new RoWordNet();
		List<String> synonyms = rown.getSynonyms("mașină");

		assertEquals("mașină-unealtă", synonyms.get(0));
		assertEquals("automobil", synonyms.get(1));
		assertEquals("locomotivă", synonyms.get(2));
	}
	
	@Test
	public void testCopacHypo() {
		RoWordNet rown = new RoWordNet();
		List<String> hyponyms = rown.getHyponyms("copac");

		assertEquals("piper_de_Guineea", hyponyms.get(0));
		assertEquals("anason", hyponyms.get(1));
	}
	
	@Test
	public void testCopacHyper() {
		RoWordNet rown = new RoWordNet();
		List<String> hypernyms = rown.getHypernyms("copac");

		assertEquals("plantă_lemnoasă", hypernyms.get(0));
	}
}
