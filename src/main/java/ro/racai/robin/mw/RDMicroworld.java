/**
 * 
 */
package ro.racai.robin.mw;

import ro.racai.robin.dialog.RDConcept;
import ro.racai.robin.dialog.RDPredicate;
import ro.racai.robin.dialog.RDUniverse;
import ro.racai.robin.nlp.WordNet;

/**
 * @author Radu Ion ({@code radu@racai.ro})
 * <p>All micro-world builders have to implement this interface.</p>
 */
public interface RDMicroworld {
	/**
	 * <p>Construct a universe from your source of choice.
	 * You can even write Java code to construct the universe.</p>
	 * @param wn    the WordNet object to be used in the creation
	 *              of the {@link RDUniverse} object.  
	 * @return      the constructed universe, populated with bound
	 *              {@link RDConcept}s and {@link RDPredicate}s.
	 */
	public RDUniverse constructUniverse(WordNet wn);
	
	/**
	 * <p>To pretty-print this micro-world, get its name.</p>
	 * @return      the name of this micro-world.
	 */
	public String getMicroworldName();
}
