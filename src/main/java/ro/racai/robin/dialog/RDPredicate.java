/**
 * 
 */
package ro.racai.robin.dialog;

import java.util.ArrayList;
import java.util.List;

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
 * <li>if the predicate has all but one arguments bound, the universe of discourse is
 * used to find the value for the remaining argument such that the predicate is {@code true};</li>
 * <li>if more than one argument remained unbound, the dialogue manager asks new questions
 * that can provide the data to bind the remaining arguments.</li>
 * </ol>
 */
public class RDPredicate {
	/**
	 * The main "name" for this predicate.
	 * It is a lemma, usually extracted from 
	 * the question (first informative verb). 
	 */
	private String actionVerb;
		
	/**
	 * Alternate names for this predicate.
	 */
	private List<String> synonymsOfActionVerb = new ArrayList<String>();
	
	/**
	 * The arguments of this predicate, in
	 * no special order.
	 */
	private List<RDConcept> predicateArguments = new ArrayList<RDConcept>();
}
