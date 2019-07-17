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
 * to actual, textual descriptions of existing objects. For instance,
 * in our PRECIS scenario, for the <i>sală</i> concept, we could have
 * values such as <i>209</i>, <i>laboratorul de SDA</i>, etc.</p>
 * <p>This class has to be sub-classed so that the correspondence
 * is made by database interrogation, XML files, etc.</p>
 */
public abstract class RDUniverse {
	protected Map<RDConcept, List<String>> conceptToLiteral;
	
	public abstract void mapConcept(RDConcept concept);
}
