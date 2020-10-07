/**
 * 
 */
package ro.racai.robin.nlp;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.junit.Test;

import ro.racai.robin.dialog.RoSayings;
import ro.racai.robin.nlp.TextProcessor.Token;

/**
 * @author Radu Ion ({@code radu@racai.ro})
 *
 */
public class RoTextProcessorTest {

	@Test
	public void testTEPROLIN() {
		TextProcessor tp = new RoTextProcessor(new RoLexicon(), new RoWordNet(), new RoSayings());
		List<Token> tokens = tp.textProcessor("Unde se află laboratorul de SDA?");
		
		assertEquals(7, tokens.size());
		assertEquals("Unde", tokens.get(0).wform);
		assertEquals("advmod", tokens.get(0).drel);
		assertEquals(3, tokens.get(0).head);
	}
}
