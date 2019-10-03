/**
 * 
 */
package ro.racai.robin.nlp;

import static org.junit.Assert.*;

import java.util.List;

import org.junit.Test;

import ro.racai.robin.dialog.RoSayings;
import ro.racai.robin.nlp.RoLexicon;
import ro.racai.robin.nlp.RoTextProcessor;
import ro.racai.robin.nlp.TextProcessor;
import ro.racai.robin.nlp.TextProcessor.Token;

/**
 * @author Radu Ion ({@code radu@racai.ro})
 *
 */
public class RoTextProcessorTest {

	@Test
	public void testTEPROLIN() {
		TextProcessor tp = new RoTextProcessor(new RoLexicon(), new RoSayings());
		List<Token> tokens = tp.textProcessor("Unde se află laboratorul de SDA?");
		
		assertTrue(tokens.size() == 7);
		assertTrue(tokens.get(0).wform.equals("Unde"));
		assertTrue(tokens.get(0).drel.equals("advmod"));
		assertTrue(tokens.get(0).head == 3);
	}
}
