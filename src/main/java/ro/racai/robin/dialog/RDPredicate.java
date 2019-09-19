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
 * <p>This is the principal information unit that the
 * dialogue system tries to determine if it is {@code true}
 * or {@code false} within the {@link RDUniverse} we are reasoning with.<p>
 * <p>A predicate is composed of:</p>
 * <ul>
 * <li>a predicate name along with its synonyms, e.g. "afla"
 * "costa" or special predicates such as "locationOf", "timeOf",
 * "whoIs", etc.</li>
 * <li>a list of predicate arguments, all of type {@link RDConcept}</li>
 * </ul>
 * <p>Each question/query is transformed automatically in such a predicate such that:
 * <ol>
 * <li>if the predicate has all arguments bound, it is a "yes/no" question and the
 * universe of discourse is used to see if predicate is {@code true} or {@code false};</li>
 * <li>if the predicate has some of the arguments unbound, the universe of discourse is
 * used to find the value for the remaining arguments such that the predicate is {@code true};</li>
 * <li>if more than one predicate match, the dialogue manager asks new questions
 * that can provide the data for disambiguation.</li>
 * </ol>
 */
public class RDPredicate {
	/**
	 * Type of user intention.
	 */
	private UIntentType userIntention;
	
	/**
	 * The main "name" for this predicate.
	 * It is a lemma, usually extracted from 
	 * the question (first informative verb). 
	 */
	private String actionVerb;
	
	/**
	 * Alternate names for the {@link #actionVerb}.
	 */
	private List<String> synonymsOfActionVerb;
	
	/**
	 * The arguments of this predicate, in
	 * no special order.
	 */
	private List<RDConcept> predicateArguments;
	
	/**
	 * @author Radu Ion ({@code radu@racai.ro})
	 * <p>A predicate match object with the overall match score
	 * and individual arguments match scores.</p>
	 */
	public static class PMatch {
		public float pMatchScore;
		
		/**
		 * This list has the same number of elements as the
		 * {@link RDPredicate#predicateArguments} list.
		 */
		public float[] aMatchScores;
		
		public PMatch(List<RDConcept> args) {
			aMatchScores = new float[args.size()];
			pMatchScore = 0.0f;
		}
	}
	
	private RDPredicate(UIntentType uint, String verb) {
		if (StringUtils.isNullEmptyOrBlank(verb)) {
			throw new RuntimeException("Action verb cannot be null, empty or blank!");
		}
		
		actionVerb = verb.trim().toLowerCase();
		userIntention = uint;
		synonymsOfActionVerb = new ArrayList<String>();
		predicateArguments = new ArrayList<RDConcept>();
	}
	
	/**
	 * Adds a new "synonym" to this {@link #actionVerb}.
	 * @param syn      the synonym to add
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
	 * <p>Get the bound arguments of this predicate.</p>
	 * @return      a list with the predicate arguments.
	 */
	public List<RDConcept> getArguments() {
		return predicateArguments;
	}
	
	/**
	 * <p>Get the action verb of this predicate.</p>
	 * @return      the {@link #actionVerb} member field.
	 */
	public String getActionVerb() {
		return actionVerb;
	}
	
	/**
	 * <p>Convenience static method for building a predicate.</p>
	 * @param uint      user intent defined in {@link UIntentType};
	 * @param pform     canonical form (lemma) for this predicate, e.g. <i>duce</i>;
	 * @param syns      synonyms for the canonical form (may be null or empty);
	 * @param args      the list of fully instantiated {@link RDConcept}s which
	 *                  are bound already, e.g. <i>laboratorul de informatică</i>.
	 * @return          an {@link RDPredicate}.
	 */
	public static RDPredicate Builder(UIntentType uint, String pform, List<String> syns, List<RDConcept> args) {
		RDPredicate predicate = new RDPredicate(uint, pform);
		
		if (syns != null) {
			for (String s : syns) {
				predicate.addSynonym(s);
			}
		}
		
		if (args != null) {
			for (RDConcept cpt : args) {
				predicate.addArgument(cpt);
			}
		}
		
		return predicate;
	}

	/**
	 * <p>Convenience method for returning a deep copy of this object.</p>
	 * @return            an exact duplicate of this object.
	 */
	public RDPredicate DeepCopy() {
		RDPredicate predicate =
			new RDPredicate(
				userIntention,
				actionVerb != null ? new String(actionVerb) : null
			);
		
		if (synonymsOfActionVerb != null) {
			for (String s : synonymsOfActionVerb) {
				predicate.addSynonym(new String(s));
			}
		}
		
		for (RDConcept cpt : predicateArguments) {
			predicate.addArgument(cpt.DeepCopy());
		}
		
		return predicate;
	}
	
	/**
	 * <p>Tests if an arbitrary word refers to this predicate (name).</p>
	 * @param word     the word to be tested;
	 * @param wn       the interface to WordNet; if {@code null}, it is not used;
	 * @return         {@code true} if the word signals the presence of this predicate.
	 */
	public boolean isThisPredicate(String word, WordNet wn) {
		word = word.trim().toLowerCase();
		
		if (word.equals(actionVerb)) {
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

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		String strValue =  actionVerb + "(";
		
		for (int i = 0; i < predicateArguments.size() - 1; i++) {
			strValue +=
				"\"" + predicateArguments.get(i) +
				"\"/" + predicateArguments.get(i).getType() + ", ";
		}
		
		if (predicateArguments.size() >= 1) {
			strValue +=
				"\"" + predicateArguments.get(predicateArguments.size() - 1) +
				"\"/" + predicateArguments.get(predicateArguments.size() - 1).getType();
		}
		
		strValue += ")";
		return strValue;
	}
}
