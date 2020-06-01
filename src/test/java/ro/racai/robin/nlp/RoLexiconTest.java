package ro.racai.robin.nlp;

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

		assertTrue(lex.sayNumber("1").equals("unu"));
		assertTrue(lex.sayNumber("11").equals("unsprezece"));
		assertTrue(lex.sayNumber("23").equals("douăzeci și trei"));
		assertTrue(lex.sayNumber("115").equals("o sută cincisprezece"));
		assertTrue(lex.sayNumber("209").equals("două sute nouă"));
		assertTrue(lex.sayNumber("1977").equals("o mie nouă sute șaptezeci și șapte"));
		assertTrue(lex.sayNumber("2020").equals("două mii douăzeci"));
		assertTrue(lex.sayNumber("145573")
			.equals("o sută patruzeci și cinci de mii cinci sute șaptezeci și trei"));
	}

	@Test
	public void testTimeTalk() {
		Lexicon lex = new RoLexicon();

		assertTrue(lex.sayTime("02:02").equals("ora două și două minute"));
		assertTrue(lex.sayTime("02:12").equals("ora două și douăsprezece minute"));
		assertTrue(
			lex.sayTime("17:53").equals("ora șaptesprezece și cincizeci și trei de minute"));
		assertTrue(lex.sayTime("00:02").equals("ora zero și două minute"));
		assertTrue(lex.sayTime("23:12").equals("ora douăzeci și trei și douăsprezece minute"));
	}

	@Test
	public void testDateTalk() {
		Lexicon lex = new RoLexicon();

		assertTrue(lex.sayDate("01/06/2020").equals("întâi iunie două mii douăzeci"));
		assertTrue(lex.sayDate("10-09-1977").equals("zece septembrie o mie nouă sute șaptezeci și șapte"));
		assertTrue(lex.sayDate("10 mar 2001").equals("zece martie două mii unu"));
	}

	@Test
	public void testEntityMark() {
		Lexicon lex = new RoLexicon();
		Map<Integer, Pair<EntityType, Integer>> entities = lex.markEntities("sala 209");

		assertTrue(entities.containsKey(5));
		assertTrue(entities.get(5).getFirstMember() == EntityType.NUMBER);
	}
}
