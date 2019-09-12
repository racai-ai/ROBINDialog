/**
 * 
 */
package ro.racai.robin.dialog;

import java.util.ArrayList;
import java.util.List;

import ro.racai.robin.nlp.StringUtils;
import ro.racai.robin.nlp.WordNet;

/**
 * @author Radu Ion ({@code radu@racai.ro})
 * <p>This class models a concept in the ROBIN Dialogue micro-world (e.g. universe of discourse).
 * For instance, in our familiar PRECIS orientation scenario, a concept is the room where something
 * happens. A concept can be expressed through a <i>canonical form</i>, e.g. Romanian <i>cameră</i>,
 * through synonyms e.g. <i>sală</i> or through a noun phrase e.g. <i>sala de la parter</i>.
 * <p>A concept is a typed variable <b>X</b> which has to be bound to a value at runtime. For instance,
 * we could speak of <i>sala 209</i> or of <i>laboratorul de robotică</i>.
 */
public class RDConcept {
	private CType conceptType;
	
	/**
	 * Keeps the canonical form of the concept.
	 * For instance, Romanian <i>cameră</i>.
	 * TODO: No multiple meanings support yet.
	 * <b>Important: no two concepts can have the
	 * same canonical form! It is a limitation
	 * for now.</b>
	 */
	private String canonicalForm;
	
	/**
	 * Alternate words for the {@link #canonicalForm}.
	 * For instance, Romanian <i>sală</i> and <i>laborator</i>.
	 */
	private List<String> synonymsOfCanonicalForm = new ArrayList<String>();
	
	/**
	 * This is the reference of the concept from the micro-world.
	 * If no reference has been assigned yet, leave this to null. 
	 */
	private String assignedReferece;
	
	private RDConcept(CType ctyp, String cform) {
		conceptType = ctyp;
		
		if (StringUtils.isNullEmptyOrBlank(cform)) {
			throw new RuntimeException("Canonical form cannot be null, empty or blank!");
		}
		
		canonicalForm = cform.trim().toLowerCase();
	}
	
	/**
	 * <p>Convenience static method for building a concept.</p>
	 * @param ctyp      type of the concept defined in {@link CType};
	 * @param cform     canonical form (lemma) for this concept, e.g. <i>sală</i>;
	 * @param syns      synonyms for the canonical form (may be null or empty);
	 * @param ref       the assigned (reference) value to this concept:
	 *                  a list of space-separated words, e.g.
	 *                  <i>laboratorul de informatică</i>.</p>
	 * @return          an {@link RDConcept}.
	 */
	public static RDConcept Builder(CType ctyp, String cform, List<String> syns, String ref) {
		RDConcept concept = new RDConcept(ctyp, cform);
		
		if (syns != null) {
			for (String s : syns) {
				concept.addSynonym(s);
			}
		}
		
		concept.setReference(ref);
		return concept;
	}
	
	/**
	 * <p>Adds a synonym to this concept, by which the
	 * concept can be identified in text.</p>
	 * @param syn      the synonym string to be added.
	 */
	public void addSynonym(String syn) {
		if (StringUtils.isNullEmptyOrBlank(syn)) {
			throw new RuntimeException("Synonym may not be null, empty or blank!");
		}
		
		synonymsOfCanonicalForm.add(syn.trim().toLowerCase());
	}
	
	/**
	 * <p>Sets the reference for this concept.</p>
	 * @param value        the reference to be set
	 */
	public void setReference(String value) {
		assignedReferece = value;
	}
	
	public String getReference() {
		return assignedReferece;
	}

	/**
	 * <p>Tests if an arbitrary word refers to this concept.</p>
	 * @param word     the word to be tested;
	 * @param wn       the interface to WordNet; if {@code null}, it is not used;
	 * @return         {@code true} if the word signals the presence of this concept.
	 */
	public boolean isThisConcept(String word, WordNet wn) {
		word = word.trim().toLowerCase();
		
		if (word.equals(canonicalForm)) {
			return true;
		}
		
		for (String syn : synonymsOfCanonicalForm) {
			if (word.equals(syn)) {
				return true;
			}
		}
		
		if (wn != null) {
			return wn.wordnetEquals(word, canonicalForm);
		}
		
		return false;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return canonicalForm;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (obj instanceof RDConcept) {
			RDConcept rdc = (RDConcept) obj;
			
			if (rdc.canonicalForm.equals(canonicalForm)) {
				if (rdc.assignedReferece == null && assignedReferece == null) {
					return true;
				}
				else if (
					rdc.assignedReferece != null && assignedReferece != null &&
					rdc.assignedReferece.equalsIgnoreCase(assignedReferece)
				) {
					return true;
				}
			}
		}

		return false;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		return canonicalForm.hashCode();
	}
}
