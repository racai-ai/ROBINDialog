package ro.racai.robin;

import static org.junit.Assert.assertEquals;
import org.junit.BeforeClass;
import org.junit.Test;
import ro.racai.robin.dialog.RDManager;
import ro.racai.robin.dialog.RDManager.DialogueState;

public class SalesTest {
    private static RDManager application;

    @BeforeClass
    public static void setup() {
        application = RDManager.createApplication("src\\main\\resources\\sales.mw");
    }

    private DialogueState getAnswer(String prompt) {
        return application.doConversation(application.processPrompt(prompt));
    }

    @Test
    public void test1() {
        DialogueState dstat = getAnswer("Aveți laptop Apple MacBook Air 13?");

        assertEquals("Da așa este.", dstat.getReply().get(0));
    }

    @Test
    public void test2() {
        DialogueState dstat = getAnswer("Ce preț are calculatorul Gaming Pro 377?");

        assertEquals("2700 de lei", dstat.getReply().get(0));
    }

    @Test
    public void test3() {
        DialogueState dstat = getAnswer("Cât costă laptopul Asus X515MA?");

        assertEquals("1399 de lei", dstat.getReply().get(0));
    }

    @Test
    public void test_context() {
        DialogueState dstat = getAnswer("Aveți laptop Apple MacBook Air 13?");

        assertEquals("Da așa este.", dstat.getReply().get(0));

        dstat = getAnswer("Câtă memorie are?");

        assertEquals("8 GB", dstat.getReply().get(0));
    }
}
