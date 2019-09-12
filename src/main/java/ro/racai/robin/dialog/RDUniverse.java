/**
 * 
 */
package ro.racai.robin.dialog;

import java.util.ArrayList;
import java.util.List;

import ro.racai.robin.nlp.TextProcessor.Query;
import ro.racai.robin.nlp.TextProcessor.Token;
import ro.racai.robin.nlp.Levenshtein;
import ro.racai.robin.nlp.WordNet;

/**
 * @author Radu Ion ({@code radu@racai.ro})
 * <p>This class describes the universe of discourse for a given
 * micro-word. That is, it maps {@link ro.racai.robin.dialog.RDConcept}s
 * to actual, textual descriptions of existing objects, being an <b>inventory</b>
 * of ``known'' values for all concepts. For instance,
 * in our PRECIS scenario, for the <i>sală</i> concept, we could have
 * values such as <i>209</i>, <i>laboratorul de SDA</i>, etc.</p>
 * <p>This class has to be sub-classed so that the correspondence
 * is made by database interrogation, XML files, etc.</p>
 */
public class RDUniverse {
	/**
	 * Bound concepts in this universe of discourse.
	 * Fill in this list using {@link #addConcept(RDConcept)}. 
	 */
	protected List<RDConcept> concepts;
	
	/**
	 * Predicates that are true in this universe.
	 * Use {@link #addPredicate()} method to fill in this list. 
	 */
	protected List<RDPredicate> predicates;
	protected Levenshtein wordDistance;
	protected WordNet wordNet;
		
	/**
	 * <p>Universe of discourse constructor.</p>
	 * @param wn      a WordNet instance for your language.
	 */
	public RDUniverse(WordNet wn) {
		concepts = new ArrayList<RDConcept>();
		predicates = new ArrayList<RDPredicate>();
		wordDistance = new Levenshtein();
		wordNet = wn;
	}
	
	/**
	 * <p>Adds a concept built with {@link RDConcept#Builder(CType, String, List, String)} to
	 * this universe of discourse. Note that the textual description {@link RDConcept#getReference()}
	 * must not be null!
	 * @param concept       the bound (instantiated) concept to be added to this universe 
	 */
	public void addConcept(RDConcept concept) {
		concepts.add(concept);
	}
	
	/**
	 * <p>Adds a ``true'' predicate to this universe of discourse.
	 * @param predicate     the predicate to add to this universe
	 */
	public void addPredicate(RDPredicate predicate) {
		predicates.add(predicate);
	}
	
	/**
	 * <p>Returns the predicate from this universe of discourse
	 * which best matched the query.</p>
	 * @param query             the parsed {@link Query} object from the
	 *                          user utterance;
	 * @return                  an instance of the 
	 *                          of mappings for the concept.
	 */
	public RDPredicate resolveQuery(Query query) {
		// TODO: NOT READY!
		return null;
	}
	
	protected boolean isConceptInstance(List<Token> userTokens, RDConcept boundConcept) {
		for (Token tok : userTokens) {
			if (tok.isActionVerbDependent) {
				if (
					boundConcept.isThisConcept(tok.lemma, wordNet) ||
					boundConcept.isThisConcept(tok.wform, wordNet)
				) {
					return true;
				}
			}
		}
		
		return false;
	}
	
	protected float matchQueryWithPredicate(Query query, RDPredicate pred) {
		// TODO: NOT READY!
		// 1. Match the action verb of the query with the one of the predicate
		if (!pred.isThisPredicate(query.actionVerb, wordNet)) {
			return 0.0f;
		}
		
		// 2. Match the syntactic arguments with logical (bound) arguments
		float mScore = 0.0f;
		// Predicate bound arguments
		List<RDConcept> predArgs = pred.getArguments();
		// User query tokens making up syntactic arguments of the verb
		List<List<Token>> qverbArgs = query.predicateArguments;
		
		for (int i = 0; i < predArgs.size(); i++) {
			RDConcept boundArg = predArgs.get(i);
			float bestSim = 0.0f;
			
			for (int j = 0; j < qverbArgs.size(); i++) {
				List<Token> userArg = qverbArgs.get(j);
				
				if (isConceptInstance(userArg, boundArg)) {
					float sim = descriptionSimilarity(boundArg.getReference(), userArg);
				}
			}
		}
		
		return 0.0f;
	}
	
	
	/**
	 * <p>Detects if two lists of words are ``similar''. Word matching
	 * is done in a lower-case manner, using string equality, WordNet and
	 * Levenshtein distances.</p>
	 * <p>If {@code i, j} are the indexes of the words aligning with
	 * the lowest Levenshtein distance L, we output
	 * sum((|i - j| + 1) * (L + 1)) / length(description).</p>
	 * @param description         the description that is to be matched;
	 * @param vtokens             list of value tokens parsed in the {@link Query};
	 * @return                    a real number that is 1.0f if the two entities
	 *                            are exactly equal and less than 1 for a degree of
	 *                            similarity.
	 */
	protected float descriptionSimilarity(String description, List<Token> vtokens) {
		String[] dtokens = description.trim().toLowerCase().split("\\s+");
		int sum = 0;
		
		for (int i = 0; i < dtokens.length; i++) {
			int L = 1000;
			int j = vtokens.size();
			String wi = dtokens[i];
			
			for (int jj = 0; jj < vtokens.size(); jj++) {
				String wjj = vtokens.get(jj).wform;
				String ljj = vtokens.get(jj).lemma;
				
				if (
					wi.equalsIgnoreCase(wjj) ||
					wi.equalsIgnoreCase(ljj) ||
					wordNet.wordnetEquals(wi, ljj)
				) {
					L = 0;
					j = jj;
					break;
				}
				else {
					int d = wordDistance.distance(wi.toLowerCase(), wjj.toLowerCase(), 5);
					
					if (d < L) {
						L = d;
						j = jj;
					}
					
					d = wordDistance.distance(wi.toLowerCase(), ljj.toLowerCase(), 5);
					
					if (d < L) {
						L = d;
						j = jj;
					}
				}
			} // end jj
			
			sum += (Math.abs(i - j) + 1) * (L + 1);
		} // end i
		
		return (float) sum / (float) dtokens.length;
	}
}
