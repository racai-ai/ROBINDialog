/**
 * 
 */
package ro.racai.robin.dialog;

import java.util.List;

/**
 * @author Radu Ion ({@code radu@racai.ro})
 *         <p>
 *         Interface to check for fixed expressions, to be extended in each new language.
 *         </p>
 */
public interface RDSayings {
	/**
	 * User has started the dialogue with a "Hello" or "Hello Pepper" or similar.
	 * 
	 * @param words a list of words to check if they mark the start of the conversation (SOC)
	 * @return {@code true} if words mark the SOC
	 */
	public boolean userOpeningStatement(List<String> words);

	/**
	 * User has ended the dialogue with a "Thank you" or "Goodbye".
	 * 
	 * @param words a list of words to check if they mark the end of the conversation (EOC)
	 * @return {@code true} if words mark the EOC
	 */
	public boolean userClosingStatement(List<String> words);

	/**
	 * What the robot says to start the conversation.
	 * 
	 * @return the list of string to be said; each string is sent separately to the TTS module.
	 */
	public List<String> robotOpeningLines();

	/**
	 * What the robot says to end the conversation.
	 * 
	 * @return the list of string to be said; each string is sent separately to the TTS module.
	 */
	public List<String> robotClosingLines();

	/**
	 * What the robot says for "I don't know."
	 * 
	 * @return the list of string to be said; each string is sent separately to the TTS module.
	 */
	public List<String> robotDontKnowLines();

	/**
	 * What the robot says for "I didn't understand."
	 * 
	 * @return the list of string to be said; each string is sent separately to the TTS module.
	 */
	public List<String> robotDidntUnderstandLines();
}
