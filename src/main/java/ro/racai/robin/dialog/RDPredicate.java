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
 *         <p>
 *         This is the principal information unit that the dialogue system tries to determine if it
 *         is {@code true} or {@code false} within the {@link RDUniverse} we are reasoning with.
 *         <p>
 *         <p>
 *         A predicate is composed of:
 *         </p>
 *         <ul>
 *         <li>a predicate name along with its synonyms, e.g. "afla" "costa" or special predicates
 *         such as "locationOf", "timeOf", "whoIs", etc.</li>
 *         <li>a list of predicate arguments, all of type {@link RDConcept}</li>
 *         </ul>
 *         <p>
 *         Each question/query is transformed automatically in such a predicate such that:
 *         <ol>
 *         <li>if the predicate has all arguments bound, it is a "yes/no" question and the universe
 *         of discourse is used to see if predicate is {@code true} or {@code false};</li>
 *         <li>if the predicate has some of the arguments unbound, the universe of discourse is used
 *         to find the value for the remaining arguments such that the predicate is
 *         {@code true};</li>
 *         <li>if more than one predicate match, the dialogue manager asks new questions that can
 *         provide the data for disambiguation.</li>
 *         </ol>
 */
public class RDPredicate {
	/**
	 * Type of user intention.
	 */
	private UIntentType userIntention;

	/**
	 * The main "name" for this predicate. It is a lemma, usually extracted from the question (first
	 * informative verb).
	 */
	private String actionVerb;

	/**
	 * Alternate names for the {@link #actionVerb}.
	 */
	private List<String> synonymsOfActionVerb;

	/**
	 * The arguments of this predicate, in no special order.
	 * To be populated from TRUE definitions.
	 */
	private List<RDConcept> predicateArguments;

	/**
	 * @author Radu Ion ({@code radu@racai.ro})
	 *         <p>
	 *         A predicate match object with the overall match score and individual arguments match
	 *         scores.
	 *         </p>
	 */
	public static class PMatch {
		/**
		 * The predicate which matched.
		 */
		public RDPredicate matchedPredicate;

		/**
		 * For a predicate to match, at least one referenced argument must match against what user
		 * said.
		 */
		public boolean isValidMatch;

		/**
		 * The score with which the predicate {@link #pMatched} matched.
		 * 
		 */
		public float matchScore;

		/**
		 * This list has the same number of elements as the {@link RDPredicate#predicateArguments}
		 * list.
		 */
		public float[] argMatchScores;

		/**
		 * When the predicate is matched, this indexes into the
		 * {@link RDPredicate#predicateArguments} list.
		 * 
		 */
		public int saidArgumentIndex;

		public PMatch(RDPredicate pred) {
			matchedPredicate = pred;
			argMatchScores = new float[pred.getArguments().size()];

			for (int i = 0; i < argMatchScores.length; i++) {
				argMatchScores[i] = 0.0f;
			}

			matchScore = 0.0f;
			saidArgumentIndex = -1;
			isValidMatch = false;
		}

		/**
		 * For a predicate match, if all bound arguments were matched, this returns {@code true}.
		 * This is a valid "Yes." answer.
		 * @return {@code true} if all arguments were matched.
		 */
		public boolean isFullMatch() {
			int matchCount = 0;

			for (int i = 0; i < argMatchScores.length; i++) {
				if (argMatchScores[i] > 0.0f) {
					matchCount++;
				}
			}

			return matchCount == argMatchScores.length;
		}

		/**
		 * If all arguments have matched but at least one of them is a
		 * Java runtime reference, we cannot answer "Yes."
		 * @return {@code true} if {@link #matchedPredicate} contains a Java reference.
		 */
		public boolean containsJavaReference() {
			for (RDConcept arg : matchedPredicate.getArguments()) {
				if (arg.hasJavaClassReference()) {
					return true;
				}
			}

			return false;
		}
	} // end PMatch

	private RDPredicate(UIntentType uint, String verb) {
		if (StringUtils.isNullEmptyOrBlank(verb)) {
			throw new RuntimeException("Action verb cannot be null, empty or blank!");
		}

		actionVerb = verb.trim().toLowerCase();
		userIntention = uint;
		synonymsOfActionVerb = new ArrayList<>();
		predicateArguments = new ArrayList<>();
	}

	/**
	 * Adds a new "synonym" to this {@link #actionVerb}.
	 * 
	 * @param syn the synonym to add
	 */
	public void addSynonym(String syn) {
		if (StringUtils.isNullEmptyOrBlank(syn)) {
			throw new RuntimeException("Synonym may not be null, empty or blank!");
		}

		synonymsOfActionVerb.add(syn.trim().toLowerCase());
	}

	public void addArgument(RDConcept arg) {
		predicateArguments.add(arg);
	}

	/**
	 * <p>
	 * Get the bound arguments of this predicate.
	 * </p>
	 * 
	 * @return a list with the predicate arguments.
	 */
	public List<RDConcept> getArguments() {
		return predicateArguments;
	}

	/**
	 * <p>
	 * Get the action verb of this predicate.
	 * </p>
	 * 
	 * @return the {@link #actionVerb} member field.
	 */
	public String getActionVerb() {
		return actionVerb;
	}

	/**
	 * <p>
	 * Get the user intent associated with this predicate.
	 * </p>
	 * 
	 * @return the {@link #userIntention} member field.
	 */
	public UIntentType getUserIntent() {
		return userIntention;
	}

	/**
	 * <p>
	 * Convenience static method for building a predicate with no bound arguments.
	 * Use this to build {@link RDPredicate} objects from PREDICATE definitions.
	 * </p>
	 * 
	 * @param uint  user intent defined in {@link UIntentType};
	 * @param pform canonical form (lemma) for this predicate, e.g. <i>duce</i>;
	 * @param syns  synonyms for the canonical form (may be null or empty);
	 * @return an {@link RDPredicate}.
	 */
	public static RDPredicate predicateBuilder(UIntentType uint, String pform, List<String> syns) {
		RDPredicate predicate = new RDPredicate(uint, pform);

		if (syns != null) {
			for (String s : syns) {
				predicate.addSynonym(s);
			}
		}

		return predicate;
	}

	/**
	 * <p>
	 * Convenience method for returning a deep copy of this object.
	 * Arguments are not copied, if they exist. Use this to instantiate predicates
	 * from the TRUE definitions.
	 * </p>
	 * 
	 * @return an exact duplicate of this object.
	 */
	public RDPredicate deepCopy() {
		RDPredicate predicate =
				new RDPredicate(userIntention, actionVerb != null ? actionVerb : null);

		if (synonymsOfActionVerb != null) {
			for (String s : synonymsOfActionVerb) {
				predicate.addSynonym(s);
			}
		}

		return predicate;
	}

	public boolean isTheActionVerb(String word) {
		return word.equalsIgnoreCase(actionVerb);
	}

	/**
	 * <p>
	 * Tests if an arbitrary word refers to this predicate (name).
	 * </p>
	 * 
	 * @param word the word to be tested;
	 * @param wn   the interface to WordNet; if {@code null}, it is not used;
	 * @return {@code true} if the word signals the presence of this predicate.
	 */
	public boolean isThisPredicate(String word, WordNet wn) {
		word = word.trim().toLowerCase();

		if (isTheActionVerb(word)) {
			return true;
		}

		for (String syn : synonymsOfActionVerb) {
			if (word.equals(syn)) {
				return true;
			}
		}

		if (wn != null) {
			return wn.wordnetEquals(word, actionVerb);
		}

		return false;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		StringBuilder strValue = new StringBuilder(actionVerb + "(");

		for (int i = 0; i < predicateArguments.size() - 1; i++) {
			strValue.append(predicateArguments.get(i) + ", ");
		}

		if (!predicateArguments.isEmpty()) {
			strValue.append(predicateArguments.get(predicateArguments.size() - 1));
		}

		strValue.append(")");
		return strValue.toString();
	}
}
