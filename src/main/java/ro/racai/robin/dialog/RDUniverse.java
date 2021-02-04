/**
 * 
 */
package ro.racai.robin.dialog;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
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
 */
public class RDUniverse {
	/**
	 * Bound concepts (defined with REFERENCE or constants) in this universe of discourse.
	 * Fill in this list using {@link #addBoundConcept(RDConcept)}.
	 */
	private List<RDConcept> boundConcepts;

	/**
	 * Concept definitions (using CONCEPT keyword) that hold in this universe of discourse.
	 */
	private List<RDConcept> definedConcepts;

	/**
	 * Predicates that are TRUE in this universe. Use {@link #addPredicate()} method to fill in this
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
	 * A static mapping attempting to correct ASR errors
	 * of the type e.g. "pe păr -> Pepper".
	 */
	private Map<String, String> asrCorrectionRules;
	
	/**
	 * <p>
	 * Universe of discourse constructor.
	 * </p>
	 * 
	 * @param wn  a WordNet instance for your language;
	 * @param lex a lexicon instance for your language.
	 */
	public RDUniverse(WordNet wn, Lexicon lex, TextProcessor proc) {
		boundConcepts = new ArrayList<>();
		definedConcepts = new ArrayList<>();
		predicates = new ArrayList<>();
		wordDistance = new Levenshtein();
		wordNet = wn;
		lexicon = lex;
		textProcessor = proc;
		asrCorrectionRules = new HashMap<>();
	}

	/**
	 * <p>Adds a wrongly recognized source phrase and its correct
	 * equivalent to the rule map.
	 * @param wrong   a phrase (space-separated) of lower-cased words
	 * @param correct the correct corresponding phrase
	 */
	public void addASRRule(String wrong, String correct) {
		asrCorrectionRules.put(wrong, correct);
	}

	public Map<String, String> getASRRulesMap() {
		return asrCorrectionRules;
	}

	public void setASRRulesMap(Map<String, String> dictionary) {
		asrCorrectionRules = dictionary;
	}

	/**
	 * <p>
	 * Get the universe instantiated concepts to pass on to the text processor or to print.
	 * </p>
	 * 
	 * @return the list of bound concepts that exist in this universe.
	 */
	public List<RDConcept> getBoundConcepts() {
		return boundConcepts;
	}

	public List<RDConcept> getDefinedConcepts() {
		return definedConcepts;
	}

	/**
	 * <p>
	 * Get the universe instantiated predicates to print.
	 * </p>
	 * 
	 * @return the list of bound predicates that exist in this universe.
	 */
	public List<RDPredicate> getBoundPredicates() {
		return predicates;
	}

	/**
	 * <p>
	 * Adds a bound concept to this universe of discourse.
	 * Note that the textual description {@link RDConcept#getReference()} must not be null or empty!
	 * </p>
	 * 
	 * @param conc the bound (instantiated) concept to be added to this universe
	 */
	public void addBoundConcept(RDConcept conc) {
		boundConcepts.add(conc);
	}

	public void addConcept(RDConcept conc) {
		definedConcepts.add(conc);
	}

	/**
	 * <p>
	 * Adds a ``true'' predicate to this universe of discourse.
	 * </p>
	 * 
	 * @param pred the predicate to add to this universe
	 */
	public void addBoundPredicate(RDPredicate pred) {
		predicates.add(pred);
	}

	public void addBoundPredicates(List<RDPredicate> preds) {
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
		PMatch result = new PMatch(pred, query.hasQueryVariable());

		for (Argument qArg : queryArgs) {
			if (qArg.isQueryVariable) {
				for (int i = 0; i < predArgs.size(); i++) {
					RDConcept pArg = predArgs.get(i);

					if (isOfSameQueryType(pArg, qArg, query.queryType)) {
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
	 * Verifies if the user description of a concept (which is a query {@link Argument}) matches the
	 * given bound concept. Also checks for type equality.
	 * </p>
	 * 
	 * @param argument     the user description of the concept;
	 * @param boundConcept the target bound concept to do the matching against.
	 * @return {@code true} if description matches the concept.
	 */
	private boolean isConceptInstance(Argument argument, RDConcept boundConcept) {
		List<RDConcept> argumentConcepts = findSimilarBoundConcepts(argument);

		for (RDConcept argumentConcept : argumentConcepts) {
			// Check for type equality first!
			if (boundConcept.getType().equals(argumentConcept.getType())
					&& (boundConcept instanceof RDConstant
							|| boundConcept.isThisConcept(argumentConcept, wordNet))) {
				// If bound concept is a constant,
				// let the description similarity tell the match story.
				return true;
			}
		}

		return false;
	}
	
	/**
	 * <p>
	 * Will say {@code true} if this concept is of the same type as the query, that is if query
	 * could be asking for this concept.
	 * </p>
	 * 
	 * @param con the concept to be matched against;
	 * @param arg the argument that was extracted from the user's query;
	 * @return {@code true} if query is asking for this.
	 */
	public boolean isOfSameQueryType(RDConcept con, Argument arg, QType typ) {
		if (arg.isQueryVariable) {
			// Go to the top-level, non-ISA concept to do comparisons.
			while (con.getType() == CType.ISA && con.getSuperClass() != null) {
				con = con.getSuperClass();
			}

			if (typ == QType.WHAT) {
				for (Token t : arg.argTokens) {
					if (t.isActionVerbDependent && lexicon.isNounPOS(t.pos)
							&& con.isThisConcept(t.lemma, wordNet)) {
						return true;
					}
				}
			}
			else if ((typ == QType.PERSON && con.getType() == CType.PERSON)
					|| (typ == QType.LOCATION && con.getType() == CType.LOCATION)
					|| (typ == QType.TIME && con.getType() == CType.TIME)) {
				return true;
			}
		}

		return false;
	}

	/**
	 * Finds the bound concepts which resemble the given {@code arg}ument.
	 * 
	 * @param arg the query argument;
	 * @return {@link RDConcept}s that are similar to the given argument.
	 */
	public List<RDConcept> findSimilarBoundConcepts(Argument arg) {
		List<RDConcept> result = new ArrayList<>();

		// Let's see if we can be a bit more specific than CType.WORD.
		// We only compare the noun heads of the reference vs. the argument.
		for (RDConcept c : boundConcepts) {
			for (Token t1 : c.getTokenizedReference()) {
				if (t1.drel.equals("root") && lexicon.isNounPOS(t1.pos)) {
					for (Token t2 : arg.argTokens) {
						if (t2.isActionVerbDependent && lexicon.isNounPOS(t2.pos)
								&& (t1.wform.equalsIgnoreCase(t2.wform)
										|| t1.lemma.equalsIgnoreCase(t2.lemma))) {
							// If c has ISA type, get the superclass.
							while (c.getType() == CType.ISA && c.getSuperClass() != null) {
								c = c.getSuperClass();
							}

							result.add(c);
						}
					}
				}
			}
		}

		return result;
	}

	/**
	 * This method will return a {@link PMatch} object that describes a match
	 * between the {@code query} and a given {@code pred}icate.
	 * @param query the analyzed query that came from the NLP module.
	 * @param pred the predicated that came from the .mw file.
	 * @return a {@link PMatch} object containing match information.
	 */
	private PMatch scoreQueryAgainstPredicate(Query query, RDPredicate predicate) {
		// 1. Match the action verb of the query with the one of the predicate
		if (!predicate.isThisPredicate(query.actionVerb, wordNet)) {
			return null;
		}

		// 2. Match the syntactic arguments with logical (bound) arguments
		// Predicate bound arguments
		List<RDConcept> predicateArgs = predicate.getArguments();
		// User query tokens making up syntactic arguments of the verb
		List<Argument> queryArgs = query.predicateArguments;
		// Find the maximal sum assignment of query arguments
		// to predicate arguments
		// Matrix is symmetrical
		float[][] matchScores = new float[predicateArgs.size()][queryArgs.size()];
		Set<String> ijPairs = new HashSet<>();

		for (int i = 0; i < predicateArgs.size(); i++) {
			RDConcept pArg = predicateArgs.get(i);

			for (int j = 0; j < queryArgs.size(); j++) {
				Argument qArg = queryArgs.get(j);

				matchScores[i][j] = 0.0f;

				if (ijPairs.contains(j + "#" + i)) {
					// Matrix is symmetrical, where it can.
					matchScores[i][j] = matchScores[j][i];
				} else if (isOfSameQueryType(pArg, qArg, query.queryType)) {
					// A query type that matches argument
					// is counted as a argument match.
					matchScores[i][j] = 1.0f;

					if (pArg.hasJavaClassReference()) {
						// Also a "full" match because this a Java class reference.
						matchScores[i][j] += 1.0f;
					}

					ijPairs.add(i + "#" + j);
				} else if (isConceptInstance(qArg, pArg)) {
					// Else, the argument is fuzzy scored against user's description.
					matchScores[i][j] = descriptionSimilarity(pArg, qArg);
					ijPairs.add(i + "#" + j);
				}
			}
		}

		PMatch result = new PMatch(predicate, query.hasQueryVariable());

		// Predicate has matched with its name.
		result.matchScore = 0.0f;

		for (int i = 0; i < predicateArgs.size(); i++) {
			RDConcept pArg = predicateArgs.get(i);
			float maxScore = 0.0f;

			for (int j = 0; j < queryArgs.size(); j++) {
				Argument qArg = queryArgs.get(j);

				if (matchScores[i][j] > maxScore) {
					maxScore = matchScores[i][j];
				}

				if (isOfSameQueryType(pArg, qArg, query.queryType)
						&& result.saidArgumentIndex == -1) {
					// Only set this once.							
					result.saidArgumentIndex = i;
				}
			}

			result.matchScore += maxScore;
			result.argMatchScores[i] = maxScore;
		}

		// 1.0 for the query variable.
		// Anything extra is reference matching or Java references, the more, the better.
		result.isValidMatch = (result.matchScore > 1.0f);
		return result;
	}

	/**
	 * <p>
	 * Detects if two lists of words are ``similar''. Word matching is done in a lower-case manner,
	 * using string equality, WordNet and Levenshtein distances.
	 * </p>
	 * <p>
	 * If {@code i, j} are the indexes of the words aligning with the lowest Levenshtein distance L,
	 * we output {@code sum((|i - j| + 1) * (L + 1)) / (length(description) + length(reference))}.
	 * </p>
	 * 
	 * @param con the bound concept to get the reference from;
	 * @param arg the query argument reference;
	 * @return a real number that is 1.0f if the two entities are exactly equal and less than 1 for
	 *         a percent of similarity.
	 */
	private float descriptionSimilarity(RDConcept con, Argument arg) {
		List<Token> description = textProcessor.noFunctionalWordsFilter(con.assignedReferenceTokens);
		int dLen = description.size();
		List<Token> reference = textProcessor.noFunctionalWordsFilter(arg.argTokens);
		int rLen = reference.size();
		int[][] ldMatrix = new int[description.size()][reference.size()];
		final int maxLD = 5;

		for (int i = 0; i < description.size(); i++) {
			String li = description.get(i).lemma;
			String wi = description.get(i).wform;

			for (int j = 0; j < reference.size(); j++) {
				String wj = reference.get(j).wform;
				String lj = reference.get(j).lemma;

				ldMatrix[i][j] = maxLD + 1;

				if (li.equalsIgnoreCase(lj) || wordNet.wordnetEquals(li, lj)) {
					ldMatrix[i][j] = 0;
				} else {
					// This one returns maxLD + 1 if there's no similarity between inputs.
					int d = wordDistance.distance(wi.toLowerCase(), wj.toLowerCase(), maxLD);

					if (d < ldMatrix[i][j]) {
						ldMatrix[i][j] = d;
					}
				}
			} // end j
		} // end i

		int sum = 0;
		Set<Integer> alreadyPaired = new HashSet<>();

		for (int i = 0; i < description.size(); i++) {
			int minLD = maxLD + 1;
			int minJ = -1;

			for (int j = 0; j < reference.size(); j++) {
				if (!alreadyPaired.contains(j)) {
					if (minLD > ldMatrix[i][j]) {
						minLD = ldMatrix[i][j];
						minJ = j;
					}

					if (minLD == 0) {
						break;
					}
				}
			}

			if (minJ >= 0) {
				alreadyPaired.add(minJ);
				sum += (Math.abs(i - minJ) + 1) * (minLD + 1);
			}
		}

		float dScore = (float) sum / (float) dLen;
		float rScore = (float) sum / (float) rLen;

		if (dScore + rScore > 0.0f) {
			return 2.0f / (dScore + rScore);
		} else {
			return 0.0f;
		}
	}
}
