/**
 * 
 */
package ro.racai.robin.dialog;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import ro.racai.robin.nlp.TextProcessor.Argument;
import ro.racai.robin.nlp.TextProcessor.Query;
import ro.racai.robin.nlp.TextProcessor.Token;
import ro.racai.robin.dialog.RDPredicate.PMatch;
import ro.racai.robin.nlp.Levenshtein;
import ro.racai.robin.nlp.Lexicon;
import ro.racai.robin.nlp.QType;
import ro.racai.robin.nlp.TextProcessor;
import ro.racai.robin.nlp.WordNet;

/**
 * @author Radu Ion ({@code radu@racai.ro})
 *         <p>
 *         This class describes the universe of discourse for a given micro-word. That is, it maps
 *         {@link ro.racai.robin.dialog.RDConcept}s to actual, textual descriptions of existing
 *         objects, being an <b>inventory</b> of ``known'' values for all concepts. For instance, in
 *         our PRECIS scenario, for the <i>sală</i> concept, we could have values such as
 *         <i>209</i>, <i>laboratorul de SDA</i>, etc.
 *         </p>
 *         <p>
 *         This class has to be sub-classed so that the correspondence is made by database
 *         interrogation, XML files, etc.
 *         </p>
 */
public class RDUniverse {
	/**
	 * Bound concepts in this universe of discourse. Fill in this list using
	 * {@link #addConcept(RDConcept)}.
	 */
	private List<RDConcept> concepts;

	/**
	 * Predicates that are true in this universe. Use {@link #addPredicate()} method to fill in this
	 * list.
	 */
	private List<RDPredicate> predicates;

	/**
	 * The word distance object used to compute Levenshtein distances.
	 */
	private Levenshtein wordDistance;

	/**
	 * The WordNet object that is used to find "similar" words.
	 */
	private WordNet wordNet;

	/**
	 * The TextProcessor to use to compute a special type of sentence length.
	 */
	private TextProcessor textProcessor;

	/**
	 * Lexicon to test for functional words when matching descriptions.
	 */
	private Lexicon lexicon;

	/**
	 * <p>
	 * Universe of discourse constructor.
	 * </p>
	 * 
	 * @param wn  a WordNet instance for your language;
	 * @param lex a lexicon instance for your language.
	 */
	public RDUniverse(WordNet wn, Lexicon lex, TextProcessor proc) {
		concepts = new ArrayList<>();
		predicates = new ArrayList<>();
		wordDistance = new Levenshtein();
		wordNet = wn;
		lexicon = lex;
		textProcessor = proc;
	}

	/**
	 * <p>
	 * Get the universe instantiated concepts to pass on to the text processor or to print.
	 * </p>
	 * 
	 * @return the list of bound concepts that exist in this universe.
	 */
	public List<RDConcept> getUniverseConcepts() {
		return concepts;
	}

	/**
	 * <p>
	 * Get the universe instantiated predicates to print.
	 * </p>
	 * 
	 * @return the list of bound predicates that exist in this universe.
	 */
	public List<RDPredicate> getUniversePredicates() {
		return predicates;
	}

	/**
	 * <p>
	 * Adds a concept built with {@link RDConcept#Builder(CType, String, List, String)} to this
	 * universe of discourse. Note that the textual description {@link RDConcept#getReference()}
	 * must not be null!
	 * 
	 * @param conc the bound (instantiated) concept to be added to this universe
	 */
	public void addConcept(RDConcept conc) {
		concepts.add(conc);
	}

	/**
	 * <p>
	 * Adds a ``true'' predicate to this universe of discourse.
	 * 
	 * @param pred the predicate to add to this universe
	 */
	public void addPredicate(RDPredicate pred) {
		predicates.add(pred);
	}

	public void addPredicates(List<RDPredicate> preds) {
		predicates.clear();
		predicates.addAll(preds);
	}

	/**
	 * <p>
	 * Checks each predicate from this universe of discourse and assigns a match score.
	 * </p>
	 * 
	 * @param query the parsed {@link Query} object from the user utterance;
	 * @return the predicate match object which best matches the query; {@code null} if no predicate
	 *         matched. It's safe to say that the information is not in the Knowledge Base in this
	 *         case.
	 */
	public PMatch resolveQuery(Query query) {
		PMatch result = null;
		float maxScore = 0.0f;

		for (RDPredicate pred : predicates) {
			PMatch pm = scoreQueryAgainstPredicate(query, pred);

			if (pm != null && pm.matchScore > maxScore) {
				result = pm;
				maxScore = pm.matchScore;
			}
		}

		return result;
	}

	/**
	 * <p>
	 * If user asks something else, in the context of the first utterance, try and find some other
	 * argument of the previously matched predicate which could be the answer...
	 * </p>
	 * 
	 * @param query the parsed {@link Query} object from the user's utterance
	 * @param pred  the previously matched predicate which could hold information that the user
	 *              wants with its current, incomplete query.
	 * @return {@code null} if no information could be extracted or a new predicate match if new
	 *         information could be extracted.
	 */
	public PMatch resolveQueryInContext(Query query, RDPredicate pred) {
		// 1. Match the action verb of the query with the one of the predicate
		if (!pred.isThisPredicate(query.actionVerb, wordNet)) {
			return null;
		}

		// Predicate bound arguments
		List<RDConcept> predArgs = pred.getArguments();
		// User query tokens making up syntactic arguments of the verb
		List<Argument> queryArgs = query.predicateArguments;
		PMatch result = new PMatch(pred);

		for (Argument qArg : queryArgs) {
			if (qArg.isQueryVariable) {
				for (int i = 0; i < predArgs.size(); i++) {
					RDConcept pArg = predArgs.get(i);

					if (qArg.isQueryVariable && isOfSameType(pArg, qArg, query.queryType)) {
						result.saidArgumentIndex = i;
						break;
					}
				}

				break;
			}
		}

		if (result.saidArgumentIndex >= 0) {
			result.isValidMatch = true;
			return result;
		}

		return null;
	}

	/**
	 * <p>
	 * Verifies if the user description of a concept matches the given bound concept.
	 * </p>
	 * 
	 * @param userTokens   the user description of the concept;
	 * @param boundConcept the target bound concept to do the matching against.
	 * @return {@code true} if description matches the concept.
	 */
	private boolean isConceptInstance(List<Token> userTokens, RDConcept boundConcept) {
		for (Token tok : userTokens) {
			if (tok.isActionVerbDependent && !lexicon.isFunctionalPOS(tok.pos)
					&& (boundConcept.isThisConcept(tok.lemma, wordNet)
							|| boundConcept.isThisConcept(tok.wform, wordNet))) {
				return true;
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
		List<Argument> queryArgs = query.predicateArguments;
		// Find the maximal sum assignment of query arguments
		// to predicate arguments
		// Matrix is symmetrical
		float[][] matchScores = new float[predArgs.size()][queryArgs.size()];
		Set<String> ijPairs = new HashSet<String>();

		for (int i = 0; i < predArgs.size(); i++) {
			RDConcept pArg = predArgs.get(i);

			for (int j = 0; j < queryArgs.size(); j++) {
				Argument qArg = queryArgs.get(j);
				List<Token> qArgToks = queryArgs.get(j).argTokens;

				matchScores[i][j] = 0.0f;

				if (ijPairs.contains(j + "#" + i)) {
					// Matrix is symmetrical, where it can.
					matchScores[i][j] = matchScores[j][i];
				} else if (
				// A query type that matches argument
				// is counted as a argument match.
				qArg.isQueryVariable && isOfSameType(pArg, qArg, query.queryType)) {
					matchScores[i][j] = 1.0f;
					ijPairs.add(i + "#" + j);
				} else if (isConceptInstance(qArgToks, pArg)) {
					// Else, the argument is fuzzy scored against user's description.
					matchScores[i][j] =
							descriptionSimilarity(pArg.getTokenizedReference(), qArgToks);
					ijPairs.add(i + "#" + j);
				}
			}
		}

		PMatch result = new PMatch(pred);

		// Predicate has matched, at least with its name.
		result.matchScore = 1.0f;

		for (int i = 0; i < predArgs.size(); i++) {
			RDConcept pArg = predArgs.get(i);
			float maxScore = 0.0f;

			for (int j = 0; j < queryArgs.size(); j++) {
				Argument qArg = queryArgs.get(j);

				if (matchScores[i][j] > maxScore) {
					maxScore = matchScores[i][j];
				}

				if (qArg.isQueryVariable && isOfSameType(pArg, qArg, query.queryType) &&
				// Only set this once.
						result.saidArgumentIndex == -1) {
					result.saidArgumentIndex = i;
				}
			}

			result.matchScore += maxScore;
			result.argMatchScores[i] = maxScore;
		}

		// 1.0 for the predicate name and 1.0 of the query variable.
		result.isValidMatch = (result.matchScore > 2.0f);
		return result;
	}

	/**
	 * <p>
	 * Will say {@code true} if this concept is of the same type as the query, that is if query
	 * could be asking for this concept.
	 * 
	 * @param con  the concept to be matched against...
	 * @param qarg the argument that was extracted from the user's query;
	 * @return {@code true} if query is asking for this.
	 */
	public boolean isOfSameType(RDConcept con, Argument arg, QType qtyp) {
		if (con.getType() == CType.WORD && qtyp == QType.WHAT) {
			for (Token t : arg.argTokens) {
				if (lexicon.isNounPOS(t.pos) && t.lemma.equalsIgnoreCase(con.canonicalForm)) {
					return true;
				}
			}
		} else if (qtyp == QType.PERSON && con.getType() == CType.PERSON) {
			return true;
		} else if (qtyp == QType.LOCATION && con.getType() == CType.LOCATION) {
			return true;
		} else if (qtyp == QType.TIME && con.getType() == CType.TIME) {
			return true;
		}

		return false;
	}

	/**
	 * <p>
	 * Detects if two lists of words are ``similar''. Word matching is done in a lower-case manner,
	 * using string equality, WordNet and Levenshtein distances.
	 * </p>
	 * <p>
	 * If {@code i, j} are the indexes of the words aligning with the lowest Levenshtein distance L,
	 * we output sum((|i - j| + 1) * (L + 1)) / (length(description) + length(reference)).
	 * </p>
	 * 
	 * @param description list of description tokens that is to be matched;
	 * @param reference   list of reference tokens that is to be matched;
	 * @return a real number that is 1.0f if the two entities are exactly equal and less than 1 for
	 *         a degree of similarity.
	 */
	private float descriptionSimilarity(List<Token> description, List<Token> reference) {
		int sum = 0;
		int dLen = textProcessor.noFunctionalWordsLength(description);
		int rLen = textProcessor.noFunctionalWordsLength(reference);

		for (int i = 0; i < description.size(); i++) {
			int ld = 1000;
			int j = reference.size();
			String li = description.get(i).lemma;
			String wi = description.get(i).wform;

			if (lexicon.isFunctionalPOS(description.get(i).pos)) {
				// Skip functional words from match.
				continue;
			}

			for (int jj = 0; jj < reference.size(); jj++) {
				if (!lexicon.isFunctionalPOS(reference.get(jj).pos)) {
					// Skip functional words from match.
					String wjj = reference.get(jj).wform;
					String ljj = reference.get(jj).lemma;

					if (li.equalsIgnoreCase(ljj) || wordNet.wordnetEquals(li, ljj)) {
						ld = 0;
						j = jj;
						break;
					} else {
						int d = wordDistance.distance(wi.toLowerCase(), wjj.toLowerCase(), 5);

						if (d < ld) {
							ld = d;
							j = jj;
						}
					}
				}
			} // end jj

			sum += (Math.abs(i - j) + 1) * (ld + 1);
		} // end i

		float dScore = (float) sum / (float) dLen;
		float rScore = (float) sum / (float) rLen;

		if (dScore + rScore > 0.0f) {
			return 2.0f / (dScore + rScore);
		} else {
			return 0.0f;
		}
	}
}
