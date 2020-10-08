package ro.racai.robin.nlp;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import com.ineo.nlp.language.LanguagePipe;
import java.io.IOException;
import ssla.SSLA;

public class RoSpeechProcessing2 extends SpeechProcessing {
    private static final String SSLA_MODEL = "speech\\ssla\\models\\ro\\anca";
    private static final String MLPLA_MODEL = "speech\\mlpla\\models\\ro";
    private static final String MLPLA_CONF = "speech\\mlpla\\etc\\languagepipe.conf";
    private static final String MLPLA_OUTFILE = "input.lab";

    @Override
    public String speechToText() {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * Using SSLA by Tiberiu Boroș. See here: https://arxiv.org/pdf/1802.05583.pdf
     */
    @Override
    public File textToSpeech(String text) {
        // 1. Write the text to the lab.txt file
		try (BufferedWriter wrt = new BufferedWriter(new OutputStreamWriter(
                new FileOutputStream("input.txt"), StandardCharsets.UTF_8))) {
            wrt.write(text);
            wrt.newLine();
        }
        catch (IOException ioe) {
            ioe.printStackTrace();
            return null;
        }

        // 2. Preprocess the lab.txt file
        try (PrintStream redirect = 
                new PrintStream(new FileOutputStream(new File(MLPLA_OUTFILE)), true, "UTF-8")) {

            PrintStream original = System.out;
            System.setOut(redirect);
            String[] args = {"--process", MLPLA_CONF, MLPLA_MODEL, "input.txt"};
            LanguagePipe.main(args);
            System.setOut(original);
        }
        catch (ClassNotFoundException cnfe) {
            cnfe.printStackTrace();
            return null;
        }
        catch (InstantiationException ie) {
            ie.printStackTrace();
            return null;
        }
        catch (IllegalAccessException iae) {
            iae.printStackTrace();
            return null;
        }
        catch (IOException ioe) {
            ioe.printStackTrace();
            return null;
        }

        try {
            String[] args = {"--synthesize", SSLA_MODEL, MLPLA_OUTFILE, "out.wav", "true"};
            SSLA.main(args);

            return new File("out.wav");
        }
        catch (IOException ioe) {
            ioe.printStackTrace();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        
        return null;
    }
}
