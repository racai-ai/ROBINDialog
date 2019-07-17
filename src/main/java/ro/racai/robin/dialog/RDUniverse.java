/**
 * 
 */
package ro.racai.robin.dialog;

import java.util.List;
import java.util.Map;

/**
 * @author Radu Ion ({@code radu@racai.ro})
 * <p>This class describes the universe of discourse for a given
 * micro-word. That is, it maps {@link ro.racai.robin.dialog.RDConcept}s
 * to actual, textual descriptions of existing objects, being an <b>inventory</b>
 * of ``known'' values for all concepts. For instance,
 * in our PRECIS scenario, for the <i>sală</i> concept, we could have
 * values such as <i>209</i>, <i>laboratorul de SDA</i>, etc.</p>
 * <p>This class has to be sub-classed so that the correspondence
 * is made by database interrogation, XML files, etc.</p>
 */
public abstract class RDUniverse {
	protected Map<RDConcept, List<String>> conceptToLiteral;
	
	/**
	 * <p>Add a textual description (word or noun phrase) to
	 * this concept, such that the description is an instantiation
	 * of the concept in this universe of discourse.</p>
	 * <p><b>Do not forget to add the concept to {@link #conceptToLiteral}
	 * protected map field!</b></p>
	 * @param concept       the concept to be mapped
	 *                      (to receive new descriptions.
	 */
	public abstract void describeConcept(RDConcept concept);
	
	/**
	 * <p>Returns {@code true} if the textual description of
	 * the concept has been mapped in this universe of discourse.
	 * In other words, it will test if the variable binding is
	 * supported by this universe of discourse.</p>
	 * @param description       the textual description of the concept;
	 * @param concept           the {@link RDConcept} instance.
	 * @return                  {@code true} if description is in the list
	 *                          of mappings for the concept.
	 */
	public boolean isInstanceOf(String description, RDConcept concept) {
		if (!conceptToLiteral.containsKey(concept)) {
			// Concept has not been mapped in this universe.
			return false;
		}
		
		for (String literal : conceptToLiteral.get(concept)) {
			if (description.equalsIgnoreCase(literal)) {
				return true;
			}
		}
		
		// TODO: do partial matching, fuzzy matching, see
		// where this simple matching fails.
		return false;
	}
}
