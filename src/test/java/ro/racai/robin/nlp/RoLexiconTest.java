package ro.racai.robin.nlp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import java.util.Map;
import org.junit.Test;

/**
 * @author Radu Ion ({@code radu@racai.ro})
 */
public class RoLexiconTest {

	@Test
	public void testNumberTalk() {
		Lexicon lex = new RoLexicon();

		assertEquals("unu", lex.sayNumber("1"));
		assertEquals("unsprezece", lex.sayNumber("11"));
		assertEquals("douăzeci și trei", lex.sayNumber("23"));
		assertEquals("o sută cincisprezece", lex.sayNumber("115"));
		assertEquals("două sute nouă", lex.sayNumber("209"));
		assertEquals("o mie nouă sute șaptezeci și șapte", lex.sayNumber("1977"));
		assertEquals("două mii douăzeci", lex.sayNumber("2020"));
		assertEquals("o sută patruzeci și cinci de mii cinci sute șaptezeci și trei",
				lex.sayNumber("145573"));
	}

	@Test
	public void testTimeTalk() {
		Lexicon lex = new RoLexicon();

		assertEquals("ora două și două minute", lex.sayTime("02:02"));
		assertEquals("ora două și douăsprezece minute", lex.sayTime("02:12"));
		assertEquals("ora șaptesprezece și cincizeci și trei de minute", lex.sayTime("17:53"));
		assertEquals("ora zero și două minute", lex.sayTime("00:02"));
		assertEquals("ora douăzeci și trei și douăsprezece minute", lex.sayTime("23:12"));
	}

	@Test
	public void testDateTalk() {
		Lexicon lex = new RoLexicon();

		assertEquals("întâi iunie două mii douăzeci", lex.sayDate("01/06/2020"));
		assertEquals("zece septembrie o mie nouă sute șaptezeci și șapte", lex.sayDate("10-09-1977"));
		assertEquals("zece martie două mii unu", lex.sayDate("10 mar 2001"));
	}

	@Test
	public void testEntityMark() {
		Lexicon lex = new RoLexicon();
		Map<Integer, Pair<EntityType, Integer>> entities = lex.markEntities("sala 209");

		assertTrue(entities.containsKey(5));
		assertEquals(EntityType.NUMBER, entities.get(5).getFirstMember());
	}
}
