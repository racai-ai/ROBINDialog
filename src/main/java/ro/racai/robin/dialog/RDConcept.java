/**
 * 
 */
package ro.racai.robin.dialog;

import java.util.ArrayList;
import java.util.List;

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
	 * Keeps the canonical form of the concept.
	 * For instance, Romanian <i>cameră</i>.
	 * TODO: No multiple meanings support yet.
	 * <b>Important: no two concepts can have the
	 * same canonical form! It is a limitation
	 * for now.</b>
	 */
	private String canonicalForm;
	
	/**
	 * If not null, represents an instantiation
	 * of this concept for the use in {@link RDPredicate}.
	 * For instance, it is the <i>209</i> in <i>sala 209</i>.
	 */
	private String assignedValue;
	
	/**
	 * Alternate names for {@link #assignedValue}.
	 */
	private List<String> synonymsOfAssignedValue = new ArrayList<String>();
	
	/**
	 * Alternate words for the {@link #canonicalForm}.
	 * For instance, Romanian <i>sală</i> and <i>laborator</i>.
	 */
	private List<String> synonymsOfCanonicalForm = new ArrayList<String>();
	
	/**
	 * <p>Sets the canonical form for this concept.</p>
	 * @param form     the form to be set as canonical.
	 */
	public void setCanonicalForm(String form) {
		canonicalForm = form.trim().toLowerCase();
	}
	
	/**
	 * <p>Sets the bound value to this concept variable.</p>
	 * @param value     the value to assign to this concept
	 */
	public void setValue(String value) {
		assignedValue = value.trim().toLowerCase();
	}
	
	/**
	 * <p>Adds a synonym to this concept, by which the
	 * concept can be identified in text.</p>
	 * @param syn      the synonym string to be added.
	 */
	public void addSynonym(String syn) {
		synonymsOfCanonicalForm.add(syn.trim().toLowerCase());
	}
	
	public void addSynonymValue(String syn) {
		synonymsOfAssignedValue.add(syn.trim().toLowerCase());
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
			// Extend synonym check with WordNet
			for (String syn : wn.getSynonyms(word)) {
				if (word.equals(syn)) {
					return true;
				}
			}
			
			// Use hypernyms from WordNet (only direct hypernyms)
			for (String hyper : wn.getHypernyms(canonicalForm)) {
				if (word.equals(hyper)) {
					return true;
				}
			}
			
			// Use hyponyms from WordNet (only direct hyponyms)
			for (String hypo : wn.getHyponyms(canonicalForm)) {
				if (word.equals(hypo)) {
					return true;
				}
			}
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
			
			if (
				rdc.canonicalForm == null &&
				canonicalForm == null
			) {
				return true;
			}
			else if (
				rdc.canonicalForm != null &&
				canonicalForm != null &&
				rdc.canonicalForm.equalsIgnoreCase(canonicalForm)
			) {
				if (rdc.assignedValue == null && assignedValue == null) {
					return true;
				}
				else if (
					rdc.assignedValue != null &&
					assignedValue != null
				) {
					if (assignedValue.equalsIgnoreCase(rdc.assignedValue)) {
						return true;
					}
					else {
						for (String v : synonymsOfAssignedValue) {
							if (rdc.assignedValue.equalsIgnoreCase(v)) {
								return true;
							}
						}
						
						for (String v : rdc.synonymsOfAssignedValue) {
							if (assignedValue.equalsIgnoreCase(v)) {
								return true;
							}
						}
					}
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
