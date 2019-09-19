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
	/**
	 * This is the type of the concept, to be
	 * checked and enforced when this concept is a
	 * predicate argument.
	 */
	protected CType conceptType;
	
	/**
	 * Keeps the canonical form of the concept.
	 * For instance, Romanian <i>cameră</i>.
	 * TODO: No multiple meanings support yet.
	 * <b>Important: no two concepts can have the
	 * same canonical form! It is a limitation
	 * for now.</b>
	 */
	protected String canonicalForm;
	
	/**
	 * Alternate words for the {@link #canonicalForm}.
	 * For instance, Romanian <i>sală</i> and <i>laborator</i>.
	 */
	protected List<String> synonymsOfCanonicalForm;
	
	/**
	 * This is the reference of the concept from the micro-world.
	 * If no reference has been assigned yet, leave this to null. 
	 */
	protected String assignedReference;
	
	/**
	 * <p>This one is for creating a {@link RDConstant}.</p> 
	 * @param ctyp         the type of the concept constant.
	 */
	public RDConcept(CType ctyp, String ref) {
		conceptType = ctyp;
		synonymsOfCanonicalForm = new ArrayList<String>();
		canonicalForm = null;
		assignedReference = ref;
	}
	
	private RDConcept(CType ctyp, String cform, String ref) {
		conceptType = ctyp;
		
		if (StringUtils.isNullEmptyOrBlank(cform)) {
			throw new RuntimeException("Canonical form cannot be null, empty or blank!");
		}
		
		canonicalForm = cform.trim().toLowerCase();
		synonymsOfCanonicalForm = new ArrayList<String>();
		assignedReference = ref;
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
		RDConcept concept = new RDConcept(ctyp, cform, ref);
		
		if (syns != null) {
			for (String s : syns) {
				concept.addSynonym(s);
			}
		}

		return concept;
	}
	
	/**
	 * <p>Create a deep copy of this concept.
	 * All internal data structure are allocated on the heap
	 * for the new object.</p>
	 * 
	 * @return    a deep copy of this object.
	 */
	public RDConcept DeepCopy() {
		RDConcept concept =
			new RDConcept(
				conceptType,
				canonicalForm != null ? new String(canonicalForm) : null,
				assignedReference != null ? new String(assignedReference) : null
			);

		if (synonymsOfCanonicalForm != null) {
			for (String s : synonymsOfCanonicalForm) {
				concept.addSynonym(new String(s));
			}
		}
		
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
		assignedReference = value;
	}
	
	/**
	 * <p>Returns the textual description (reference) for this
	 * concept in the given {@link RDUniverse}.</p>
	 * @return              the textual description (or reference) of
	 *                      this concept or {@code null} if there isn't one.
	 */
	public String getReference() {
		return assignedReference;
	}

	/**
	 * <p>Returns the "standard" name for this concept.</p>
	 * @return          the {@link #canonicalForm} member field.
	 */
	public String getCanonicalName() {
		return canonicalForm;
	}
	
	/**
	 * <p>Returns the type of this concept.</p>
	 * @return         the {@link #conceptType} member field.
	 */
	public CType getType() {
		return conceptType;
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
		if (!StringUtils.isNullEmptyOrBlank(assignedReference)) {
			return assignedReference;
		}
		
		return canonicalForm;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (obj instanceof RDConcept) {
			RDConcept rdc = (RDConcept) obj;
			
			if (conceptType != rdc.conceptType) {
				// Types have to be the same, as well.
				return false;
			}
			
			if (rdc.canonicalForm.equals(canonicalForm)) {
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
