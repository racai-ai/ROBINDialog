/**
 * 
 */
package ro.racai.robin.dialog;

import ro.racai.robin.nlp.StringUtils;

/**
 * @author Radu Ion ({@code radu@racai.ro})
 * <p>A "constant" is an instance of a concept for which
 * we don't care about the name of the variable. For instance,
 * <i>8:00</i> is an instance of the "hour" concept but we only
 * keep the instance. Useful when we want to talk about these constants.</p>
 */
public class RDConstant extends RDConcept {
	/**
	 * <p>Create a constant of type {@code ctyp}.</p>
	 * @param ctyp      the type of the constant being created;
	 * @param ref       the reference for this constant, it has
	 *                  to be non-null!
	 */
	public RDConstant(CType ctyp, String ref) {
		super(ctyp, ref);
		
		if (StringUtils.isNullEmptyOrBlank(ref)) {
			throw new RuntimeException("Reference cannot be null, empty or blank!");
		}
	}
	
	@Override
	public RDConstant DeepCopy() {
		return new RDConstant(conceptType, assignedReference);
	}
	
	/**
	 * <p>Do nothing as constants do not have synonyms.</p>
	 */
	@Override
	public void addSynonym(String syn) {}

	/**
	 * <p>A constant can be equal to an instantiated concept
	 * having the same instance value and type.</p>
	 */
	@Override
	public boolean equals(Object obj) {
		if (obj instanceof RDConcept) {
			RDConcept rdc = (RDConcept) obj;
			
			if (conceptType != rdc.conceptType) {
				// Types have to be the same, as well.
				return false;
			}
			
			if (rdc.assignedReference == null && assignedReference == null) {
				return true;
			}
			else if (
				rdc.assignedReference != null && assignedReference != null &&
				rdc.assignedReference.equalsIgnoreCase(assignedReference)
			) {
				return true;
			}
		}

		return false;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		return assignedReference.hashCode();
	}
}
