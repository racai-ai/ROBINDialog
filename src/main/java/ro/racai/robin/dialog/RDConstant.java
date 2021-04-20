/**
 * 
 */
package ro.racai.robin.dialog;

/**
 * @author Radu Ion ({@code radu@racai.ro})
 *         <p>
 *         A "constant" is an instance of a concept defined as such in the micro-world file.
 *         For instance, <i>8:00</i> is an instance of the "hour" concept.
 *         Useful when we want to talk about these constants.
 *         </p>
 */
public class RDConstant extends RDConcept {
	/**
	 * <p>
	 * Create a constant of type {@code ctyp}.
	 * </p>
	 * 
	 * @param ctyp the type of the constant being created;
	 * @param ref  the reference for this constant, it has to be non-null!
	 */
	public RDConstant(CType ctyp) {
		super(ctyp);
	}

	/**
	 * <p>
	 * Do nothing as constants do not have synonyms.
	 * </p>
	 */
	@Override
	public void addSynonym(String syn) {
		// Constants do not have synonyms
	}

	/**
	 * <p>
	 * A constant can be equal to an instantiated concept having the same instance value and type.
	 * </p>
	 */
	@Override
	public boolean equals(Object obj) {
		if (obj instanceof RDConcept) {
			RDConcept rdc = (RDConcept) obj;

			if (conceptType != rdc.conceptType) {
				// Types have to be the same, as well.
				return false;
			}

			if (rdc.getReference().equals(this.getReference()) || (conceptType == CType.AMOUNT
					&& Math.abs(numericalValue - rdc.numericalValue) < 1e-5f
					&& typeOfNumericalValue.equalsIgnoreCase(rdc.typeOfNumericalValue))) {
				return true;
			}
		}

		return false;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		return getReference().hashCode();
	}

	@Override
	public String toString() {
		return "``" + assignedReference + "''" + "/" + conceptType.name();
	}
}
