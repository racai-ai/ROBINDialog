/**
 * 
 */
package ro.racai.robin.nlp;

/**
 * @author Radu Ion ({@code radu@racai.ro})
 * <p>Class dealing with string-related useful functions.</p>
 *
 */
public class StringUtils {
	private StringUtils() {
	}
	
	public static boolean isNullEmptyOrBlank(String input) {
		return input == null || input.isEmpty() || input.matches("^\\s+$");
	}
}
