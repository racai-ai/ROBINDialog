/**
 * 
 */
package ro.racai.robin.nlp;

/**
 * @author Radu Ion {@code radu@racai.ro}
 * <p>An enumeration describing possible query types.</p>
 */
public enum QType {
	// Condu-mă te rog la camera 1222.
	COMMAND,
	// Unde e sala 209?
	LOCATION,
	// Cine ține cursul din sala 1254?
	// Cine are curs în sala 1254?
	PERSON,
	// Când se ține cursul de fotografie?
	TIME,
	// Cum ajung în laboratorul de robotică?
	HOW,
	// Nu e așa că d-na Laura Florescu ține cursul X în sala Y?
	YESNO
}
