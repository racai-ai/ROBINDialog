/**
 * 
 */
package ro.racai.robin.dialog;

/**
 * @author Radu Ion ({@code radu@racai.ro})
 * <p>Concept type: if not a word such as <i>sală</i>, it can
 * be a TIME unit or a PERSON (for now; more to be added as needed).</p>
 */
public enum CType {
	// Any noun here.
	// This is the general category, the default category.
	WORD,
	// e.g. Angela Gheorghiu
	PERSON,
	// e.g. 8:15
	TIME,
	// e.g. sala 209
	LOCATION
}
