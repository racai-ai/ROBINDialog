package ro.racai.robin.nlp;

/**
 * This enumeration will specify the type of 
 * a text entity to be expanded into the target
 * language, e.g. Romanian.
 * For instance, we have to convert integer numbers
 * for sending them to the TTS engine, as in
 * 203 -> 'două sute trei'.
 */
public enum EntityType {
    NUMBER, DATE, TIME
}
