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
	 * This is the reference(s) of the concept from the micro-world.
	 * If no reference has been assigned yet, leave this to empty.
	 */
	protected List<String> assignedReferences;

	/**
	 * This is set to {@code true} if the concept reference <i>i</i> from {@link assignedReferences}
	 * is to be resolved by running the specified Java class.
	 */
	protected boolean isJavaClass;

	/**
	 * The processed version of the {@link #assignedReferences}. To be filled in at the first
	 * request.
	 */
	protected List<List<Token>> assignedReferencesTokens;

	/**
	 * <p>
	 * This one is for creating a {@link RDConstant}.
	 * </p>
	 * 
	 * @param ctyp the type of the concept constant.
	 * @param refs the list of references for the constant.
	 * @param sup  the superclass concept, if applicable.
	 */
	public RDConcept(CType ctyp, List<String> refs, RDConcept sup) {
		conceptType = ctyp;
		synonymsOfCanonicalForm = new ArrayList<>();
		canonicalForm = null;
		superClass = sup;
		assignedReferences = refs;

		if (assignedReferences == null) {
			assignedReferences = new ArrayList<>();
		}

		assignedReferencesTokens = new ArrayList<>();
	}

	/**
	 * Used to create {@link RDConstant}s.
	 * 
	 * @param ctyp the type of the constant.
	 * @param ref  the reference (value) of the constant.
	 */
	public RDConcept(CType ctyp, String ref) {
		conceptType = ctyp;
		synonymsOfCanonicalForm = new ArrayList<>();
		canonicalForm = null;
		superClass = null;
		assignedReferences = new ArrayList<>();
		assignedReferences.add(ref);
		assignedReferencesTokens = new ArrayList<>();
	}

	private RDConcept(CType ctyp, String cform, List<String> refs, RDConcept sup) {
		conceptType = ctyp;

		if (StringUtils.isNullEmptyOrBlank(cform)) {
			throw new RuntimeException("Canonical form cannot be null, empty or blank!");
		}

		canonicalForm = cform.trim().toLowerCase();
		synonymsOfCanonicalForm = new ArrayList<>();
		superClass = sup;
		assignedReferences = refs;

		if (assignedReferences == null) {
			assignedReferences = new ArrayList<>();
		}

		assignedReferencesTokens = new ArrayList<>();
	}

	/**
	 * <p>
	 * Convenience static method for building a concept.
	 * </p>
	 * 
	 * @param ctyp  type of the concept defined in {@link CType};
	 * @param cform canonical form (lemma) for this concept, e.g. <i>sală</i>;
	 * @param syns  synonyms for the canonical form (may be null or empty);
	 * @param refs  the assigned (reference) values to this concept: a list of space-separated
	 *              words, e.g. <i>laboratorul de informatică</i>, <i>laboratorul de programare</i>.
	 * @param scls  the superclass concept of this one.
	 * @return an {@link RDConcept}.
	 */
	public static RDConcept conceptBuilder(CType ctyp, String cform, List<String> syns,
			List<String> refs, RDConcept scls) {
		RDConcept concept = new RDConcept(ctyp, cform, refs, scls);

		if (syns != null) {
			for (String s : syns) {
				concept.addSynonym(s);
			}
		}

		return concept;
	}

	/**
	 * <p>
	 * Create a deep copy of this concept. All internal data structure are allocated on the heap for
	 * the new object, except for immutable types.
	 * </p>
	 * 
	 * @return a deep copy of this object.
	 */
	public RDConcept deepCopy() {
		RDConcept concept = new RDConcept(conceptType, canonicalForm, assignedReferences, superClass);

		// 1. Copy sysnonyms
		if (synonymsOfCanonicalForm != null) {
			for (String s : synonymsOfCanonicalForm) {
				concept.addSynonym(s);
			}
		}

		// 2. Copy Java class status
		concept.setJavaClass(isJavaClass);

		// 3. Copy the references processing, if it's available.
		for (List<Token> tref : getTokenizedReferences()) {
			List<Token> cref = new ArrayList<>();

			for (Token t : tref) {
				cref.add(t);
			}

			concept.getTokenizedReferences().add(cref);
		}

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
	 * Sets the references for this concept.
	 * </p>
	 * 
	 * @param values the references to be set.
	 */
	public void setReferences(List<String> values, TextProcessor proc) {
		if (values != null && !values.isEmpty()) {
			for (String value : values) {
				if (!StringUtils.isNullEmptyOrBlank(value) && !assignedReferences.contains(value)) {
					assignedReferences.add(value);

					if (value.startsWith("ro.racai.robin.dialog.generators.")) {
						isJavaClass = true;
					}

					assignedReferencesTokens.add(proc.textProcessor(value));
				}
			}
		}
	}

	/**
	 * Out of multiple, synonymous references, only use one for replying to the user.
	 * @return the preferred reference {@link String}.
	 */
	public String getPreferredReference() {
		return assignedReferences.get(0);
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
	 * Gets the tokenized versions of the references for matching with user's sayings.
	 * </p>
	 * 
	 * @return the processed version of the {@link #assignedReferences} member field.
	 */
	public List<List<Token>> getTokenizedReferences() {
		return assignedReferencesTokens;
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
		if (!assignedReferences.isEmpty()) {
			List<String> resultingString = new ArrayList<>();

			for (String assignedReference : assignedReferences) {
				if (!StringUtils.isNullEmptyOrBlank(assignedReference)) {
					resultingString.add("\"" + assignedReference + "\"" + "/" + conceptType.name());
				}
			}

			return String.join(", ", resultingString);
		}
		else {
			return "\"" + canonicalForm + "\"" + "/" + conceptType.name();
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

			if (conceptType != rdc.conceptType) {
				// Types have to be the same, as well.
				return false;
			}

			if (rdc.canonicalForm.equals(canonicalForm)) {
				if (rdc.assignedReferences.size() == this.assignedReferences.size()) {
					for (String ref : this.assignedReferences) {
						if (!rdc.assignedReferences.contains(ref)) {
							return false;
						}
					}
				}

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
