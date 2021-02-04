package ro.racai.robin;

import static org.junit.Assert.assertEquals;
import org.junit.BeforeClass;
import org.junit.Test;
import ro.racai.robin.dialog.RDManager;
import ro.racai.robin.dialog.RDManager.DialogueState;

public class PrecisTest {
	private static RDManager application;

	@BeforeClass
	public static void setup() {
		application = RDManager.createApplication("src\\main\\resources\\precis.mw");
	}
	
	private DialogueState getAnswer(String prompt) {
		return application.doConversation(application.processPrompt(prompt));
	}

	@Test
	public void test1() {
		DialogueState dstat = getAnswer("Unde se ține cursul de inteligență artificială?");

		assertEquals("sala 209", dstat.getReply().get(0));
	}

	@Test
	public void test2() {
		DialogueState dstat = getAnswer("Cine ține laboratorul de informatică?");

		assertEquals("Florin Temișan", dstat.getReply().get(0));
	}

	@Test
	public void test3() {
		DialogueState dstat = getAnswer("Când se ține laboratorul de SDA?");

		assertEquals("joia de la 10:00", dstat.getReply().get(0));
	}

	@Test
	public void test4() {
		DialogueState dstat = getAnswer("Să mă duci la sala 209.");

		assertEquals("Da așa este.", dstat.getReply().get(0));
	}
}
