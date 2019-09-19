/**
 * 
 */
package ro.racai.robin.dialog;

import java.util.ArrayList;
import java.util.List;

import ro.racai.robin.nlp.TextProcessor.Query;
import ro.racai.robin.nlp.TextProcessor.Token;
import ro.racai.robin.dialog.RDPredicate.PMatch;
import ro.racai.robin.nlp.Levenshtein;
import ro.racai.robin.nlp.Lexicon;
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
	private List<RDConcept> concepts;
	
	/**
	 * Predicates that are true in this universe.
	 * Use {@link #addPredicate()} method to fill in this list. 
	 */
	private List<RDPredicate> predicates;
		
	/**
	 * The word distance object used to compute
	 * Levenshtein distances. 
	 */
	private Levenshtein wordDistance;
		
	/**
	 * The WordNet object that is used to find
	 * "similar" words. 
	 */
	private WordNet wordNet;
	
	/**
	 * Lexicon to test for functional words
	 * when matching descriptions.
	 */
	private Lexicon lexicon;

	/**
	 * <p>Universe of discourse constructor.</p>
	 * @param wn      a WordNet instance for your language;
	 * @param lex     a lexicon instance for your language.
	 */
	public RDUniverse(WordNet wn, Lexicon lex) {
		concepts = new ArrayList<RDConcept>();
		predicates = new ArrayList<RDPredicate>();
		wordDistance = new Levenshtein();
		wordNet = wn;
		lexicon = lex;
	}
	
	/**
	 * <p>Adds a concept built with {@link RDConcept#Builder(CType, String, List, String)} to
	 * this universe of discourse. Note that the textual description {@link RDConcept#getReference()}
	 * must not be null!
	 * @param conc       the bound (instantiated) concept to be added to this universe 
	 */
	public void addConcept(RDConcept conc) {
		concepts.add(conc);
	}
	
	/**
	 * <p>Adds a ``true'' predicate to this universe of discourse.
	 * @param pred     the predicate to add to this universe
	 */
	public void addPredicate(RDPredicate pred) {
		predicates.add(pred);
	}
	
	public void addPredicates(List<RDPredicate> preds) {
		predicates.clear();
		predicates.addAll(preds);
	}
	
	/**
	 * <p>Checks each predicate from this universe of discourse
	 * and assigns a match score.</p>
	 * @param query             the parsed {@link Query} object from the
	 *                          user utterance;
	 * @return                  the predicate which best matches the query.
	 */
	public RDPredicate resolveQuery(Query query) {
		RDPredicate result = null;
		float maxScore = 0.0f;
		
		for (RDPredicate pred : predicates) {
			PMatch pm = scoreQueryAgainstPredicate(query, pred);
			
			if (pm.pMatchScore > maxScore) {
				result = pred;
				maxScore = pm.pMatchScore;
			}
		}
		
		return result;
	}
	
	private boolean isConceptInstance(List<Token> userTokens, RDConcept boundConcept) {
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
	
	private PMatch scoreQueryAgainstPredicate(Query query, RDPredicate pred) {
		// 1. Match the action verb of the query with the one of the predicate
		if (!pred.isThisPredicate(query.actionVerb, wordNet)) {
			return null;
		}
		
		// 2. Match the syntactic arguments with logical (bound) arguments
		// Predicate bound arguments
		List<RDConcept> predArgs = pred.getArguments();
		// User query tokens making up syntactic arguments of the verb
		List<List<Token>> queryArgs = query.predicateArguments;
		// Find the maximal sum assignment of query arguments
		// to predicate arguments
		float[][] matchScores = new float[predArgs.size()][queryArgs.size()];
		
		for (int i = 0; i < predArgs.size(); i++) {
			RDConcept pArg = predArgs.get(i);
			
			for (int j = 0; j < queryArgs.size(); i++) {
				List<Token> qArg = queryArgs.get(j);
				
				matchScores[i][j] = 0.0f;
				
				if (isConceptInstance(qArg, pArg)) {
					matchScores[i][j] = descriptionSimilarity(pArg.getReference(), qArg);
				}
			}
		}
		
		PMatch result = new PMatch(predArgs);
		
		for (int i = 0; i < predArgs.size(); i++) {
			float maxScore = 0.0f;
			
			for (int j = 0; j < queryArgs.size(); i++) {
				if (matchScores[i][j] > maxScore) {
					maxScore = matchScores[i][j];
				}
			}
			
			result.pMatchScore += maxScore; 
			result.aMatchScores[i] = maxScore;
		}
		
		return result;
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
	private float descriptionSimilarity(String description, List<Token> vtokens) {
		String[] dtokens = description.trim().toLowerCase().split("\\s+");
		int sum = 0;
		int dLen = 0;
		int vLen = 0;
		
		for (int i = 0; i < dtokens.length; i++) {
			int L = 1000;
			int j = vtokens.size();
			String wi = dtokens[i];
			
			if (lexicon.isFunctionalWord(wi)) {
				// Skip functional words from match.
				continue;
			}
			
			dLen++;
			
			for (int jj = 0; jj < vtokens.size(); jj++) {
				if (lexicon.isFunctionalPOS(vtokens.get(jj).POS)) {
					// Skip functional words from match.
					continue;
				}
				
				vLen++;
				
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
		
		float dScore = (float) sum / (float) dLen;
		float vScore = (float) sum / (float) vLen;
		
		return (dScore + vScore) / 2.0f;
	}
}
