/**
 * 
 */
package ro.racai.robin.dialog;

import java.util.ArrayList;
import java.util.List;
import ro.racai.robin.nlp.StringUtils;
import ro.racai.robin.nlp.TextProcessor;
import ro.racai.robin.nlp.TextProcessor.Token;
import ro.racai.robin.nlp.WordNet;

/**
 * @author Radu Ion ({@code radu@racai.ro})
 *         <p>
 *         This class models a concept in the ROBIN Dialogue micro-world (e.g. universe of
 *         discourse). For instance, in our familiar PRECIS orientation scenario, a concept is the
 *         room where something happens. A concept can be expressed through a <i>canonical form</i>,
 *         e.g. Romanian <i>cameră</i>, through synonyms e.g. <i>sală</i> or through a noun phrase
 *         e.g. <i>sala de la parter</i>.
 *         <p>
 *         A concept is a typed variable <b>X</b> which has to be bound to a value at runtime. For
 *         instance, we could speak of <i>sala 209</i> or of <i>laboratorul de robotică</i>.
 */
public class RDConcept {
	/**
	 * This is the type of the concept, to be checked and enforced when this concept is a predicate
	 * argument.
	 */
	protected CType conceptType;

	/**
	 * This is not null if {@link #conceptType} is {@link CType#ISA}.
	 */
	protected RDConcept superClass;

	/**
	 * Keeps the canonical form of the concept. For instance, Romanian <i>cameră</i>.
	 * TODO: No multiple meanings support yet.
	 * <b>Important: no two concepts can have the same canonical form! It is a limitation for now.</b>
	 */
	protected String canonicalForm;

	/**
	 * Alternate words for the {@link #canonicalForm}. For instance, Romanian <i>sală</i> and
	 * <i>laborator</i>.
	 */
	protected List<String> synonymsOfCanonicalForm;

	/**
	 * This is the reference of the concept from the micro-world.
	 * If no reference has been assigned yet, leave this to null.
	 */
	protected String assignedReference;

	/**
	 * The processed version of the {@link #assignedReferences}. To be filled in at the first
	 * request.
	 */
	protected List<Token> assignedReferenceTokens;

	/**
	 * This is set to {@code true} if the concept reference <i>i</i> from {@link assignedReferences}
	 * is to be resolved by running the specified Java class.
	 */
	protected boolean isJavaClass;

	/**
	 * Used to create {@link RDConstant}s.
	 * 
	 * @param ctyp the type of the constant.
	 */
	public RDConcept(CType ctyp) {
		conceptType = ctyp;
		synonymsOfCanonicalForm = new ArrayList<>();
		canonicalForm = null;
		superClass = null;
		assignedReference = null;
		assignedReferenceTokens = new ArrayList<>();
		isJavaClass = false;
	}

	private RDConcept(CType ctyp, String cform, RDConcept sup) {
		conceptType = ctyp;

		if (StringUtils.isNullEmptyOrBlank(cform)) {
			throw new RuntimeException("Canonical form cannot be null, empty or blank!");
		}

		canonicalForm = cform.trim().toLowerCase();
		synonymsOfCanonicalForm = new ArrayList<>();
		superClass = sup;
		assignedReference = null;
		assignedReferenceTokens = new ArrayList<>();
		isJavaClass = false;
	}

	/**
	 * <p>
	 * Convenience static method for building a concept from CONCEPT definitions.
	 * </p>
	 * 
	 * @param ctyp  type of the concept defined in {@link CType};
	 * @param cform canonical form (lemma) for this concept, e.g. <i>sală</i>;
	 * @param syns  synonyms for the canonical form (may be null or empty);
	 * @param scls  the superclass concept of this one.
	 * @return an {@link RDConcept} that has no reference assigned yet.
	 */
	public static RDConcept conceptBuilder(CType ctyp, String cform, List<String> syns, RDConcept scls) {
		RDConcept concept = new RDConcept(ctyp, cform, scls);

		if (syns != null) {
			for (String s : syns) {
				concept.addSynonym(s);
			}
		}

		return concept;
	}

	/**
	 * <p>
	 * Create a deep copy of this concept in order to assign a reference to it.
	 * All internal data structure are allocated on the heap for the new object, except for immutable types.
	 * </p>
	 * 
	 * @return a deep copy of this object, with no assigned reference.
	 */
	public RDConcept deepCopy() {
		RDConcept concept = new RDConcept(conceptType, canonicalForm, superClass);

		// 1. Copy sysnonyms
		if (synonymsOfCanonicalForm != null) {
			for (String s : synonymsOfCanonicalForm) {
				concept.addSynonym(s);
			}
		}

		// 2. Copy Java class status
		concept.setJavaClass(isJavaClass);

		return concept;
	}

	/**
	 * <p>
	 * Adds a synonym to this concept, by which the concept can be identified in text.
	 * </p>
	 * 
	 * @param syn the synonym string to be added.
	 */
	public void addSynonym(String syn) {
		if (StringUtils.isNullEmptyOrBlank(syn)) {
			throw new RuntimeException("Synonym may not be null, empty or blank!");
		}

		synonymsOfCanonicalForm.add(syn.trim().toLowerCase());
	}

	/**
	 * <p>
	 * Sets the reference for this concept from a REFERENCE definition.
	 * </p>
	 * 
	 * @param value the reference to be set.
	 */
	public void setReference(String value, TextProcessor proc) {
		if (!StringUtils.isNullEmptyOrBlank(value)) {
			assignedReference = value.trim();

			if (value.startsWith("ro.racai.robin.dialog.generators.")) {
				isJavaClass = true;
			}

			assignedReferenceTokens = proc.textProcessor(value, isJavaClass, true);
		}
	}

	/**
	 * Gets the reference. Throws {@link RuntimeException} if the reference is null or empty!
	 * One cannot use {@link RDCon}
	 * @return the reference {@link String}.
	 */
	public String getReference() {
		if (StringUtils.isNullEmptyOrBlank(assignedReference)) {
			throw new RuntimeException(String.format("RDConcept %s is not bound!", toString()));
		}

		return assignedReference;
	}
	
	/**
	 * <p>
	 * Gets the tokenized version of the reference for matching with user's sayings.
	 * </p>
	 * 
	 * @return the processed version of the {@link #assignedReference} member field.
	 */
	public List<Token> getTokenizedReference() {
		if (assignedReferenceTokens.isEmpty()) {
			throw new RuntimeException(String.format("RDConcept %s has not been processed!", toString()));
		}

		return assignedReferenceTokens;
	}

	protected void setJavaClass(boolean value) {
		isJavaClass = value;
	}

	/**
	 * Returns the value of the {@link #isJavaClass} member field. That is, if the reference of this
	 * concept is to be obtained by executing the provided Java class, return {@code true}.
	 * 
	 * @return {@code true} if the reference of this concept is a Java class name.
	 */
	public boolean hasJavaClassReference() {
		return isJavaClass;
	}

	/**
	 * <p>
	 * Returns the "standard" name for this concept.
	 * </p>
	 * 
	 * @return the {@link #canonicalForm} member field.
	 */
	public String getCanonicalName() {
		return canonicalForm;
	}

	/**
	 * <p>
	 * Returns the type of this concept.
	 * </p>
	 * 
	 * @return the {@link #conceptType} member field.
	 */
	public CType getType() {
		return conceptType;
	}

	public List<String> getSynonyms() {
		return synonymsOfCanonicalForm;
	}

	public boolean isThisConcept(RDConcept another, WordNet wn) {
		return another.equals(this) || isThisConcept(another.getCanonicalName(), wn);
	}

	/**
	 * <p>
	 * Tests if an arbitrary word refers to this concept.
	 * </p>
	 * 
	 * @param word the word to be tested;
	 * @param wn   the interface to WordNet; if {@code null}, it is not used;
	 * @return {@code true} if the word signals the presence of this concept.
	 */
	public boolean isThisConcept(String word, WordNet wn) {
		if (canonicalForm == null) {
			return false;
		}

		word = word.trim().toLowerCase();

		if (word.equals(canonicalForm)) {
			return true; 
		}

		for (String syn : synonymsOfCanonicalForm) {
			if (word.equals(syn)) {
				return true;
			}
		}

		if (wn != null && wn.wordnetEquals(word, canonicalForm)) {
			return true;
		}

		// Check IS-A relationship
		if (conceptType == CType.ISA && superClass != null) {
			if (word.equals(superClass.getCanonicalName())) {
				return true;
			}

			for (String syn : superClass.getSynonyms()) {
				if (word.equals(syn)) {
					return true;
				}
			}

			if (wn != null && wn.wordnetEquals(word, superClass.getCanonicalName())) {
				return true;
			}
		}

		return false;
	}

	public RDConcept getSuperClass() {
		return superClass;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		if (!StringUtils.isNullEmptyOrBlank(assignedReference)) {
			return  canonicalForm + "/" + conceptType.name() + " -> ``" + assignedReference + "''";
		}
		else {
			return canonicalForm + "/" + conceptType.name();
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (obj instanceof RDConcept) {
			RDConcept rdc = (RDConcept) obj;

			if (rdc == this) {
				return true;
			}

			if (conceptType != rdc.conceptType) {
				// Types have to be the same, as well.
				return false;
			}

			if (rdc.canonicalForm.equals(canonicalForm)
					&& ((this.assignedReference == null && rdc.assignedReference == null)
							|| (this.assignedReference != null && rdc.assignedReference != null
									&& this.assignedReference.equals(rdc.assignedReference)))) {
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
		return canonicalForm.hashCode();
	}
}
