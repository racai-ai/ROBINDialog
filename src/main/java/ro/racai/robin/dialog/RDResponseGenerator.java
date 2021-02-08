package ro.racai.robin.dialog;

public interface RDResponseGenerator {
    /**
     * This method will generate a text in the given language that
     * is going to be said by the TTS module.
     * @return the generated text.
     */
    public String generate();
}
