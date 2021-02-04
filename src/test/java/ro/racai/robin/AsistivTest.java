package ro.racai.robin;

import static org.junit.Assert.assertEquals;
import org.junit.BeforeClass;
import org.junit.Test;
import ro.racai.robin.dialog.RDManager;
import ro.racai.robin.dialog.RDManager.DialogueState;

public class AsistivTest {
	private static RDManager application;

	@BeforeClass
	public static void setup() {
		application = RDManager.createApplication("src\\main\\resources\\asistiv.mw");
	}

	private DialogueState getAnswer(String prompt) {
		return application.doConversation(application.processPrompt(prompt));
	}

	@Test
	public void test1() {
		DialogueState dstat = getAnswer("Cât este ceasul?");

		assertEquals("ro.racai.robin.dialog.generators.TimeNow", dstat.getReply().get(0));
	}

	@Test
	public void test2() {
		DialogueState dstat = getAnswer("Câte grade sunt afară?");

		assertEquals("ro.racai.robin.dialog.generators.DegreesNow", dstat.getReply().get(0));
	}

	@Test
	public void test3() {
		DialogueState dstat = getAnswer("Când iau aspirină?");

		assertEquals("ora șase dimineața", dstat.getReply().get(0));
	}

	@Test
	public void test4() {
		DialogueState dstat = getAnswer("Iau aspirină la ora șase dimineața?");

		assertEquals("Da așa este.", dstat.getReply().get(0));
	}
}
