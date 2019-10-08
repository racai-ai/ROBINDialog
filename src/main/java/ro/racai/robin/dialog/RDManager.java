/**
 * 
 */
package ro.racai.robin.dialog;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.stream.Collectors;

import ro.racai.robin.dialog.RDPredicate.PMatch;
import ro.racai.robin.mw.MWFileReader;
import ro.racai.robin.nlp.Lexicon;
import ro.racai.robin.nlp.QType;
import ro.racai.robin.nlp.RoLexicon;
import ro.racai.robin.nlp.RoTextProcessor;
import ro.racai.robin.nlp.RoWordNet;
import ro.racai.robin.nlp.TextProcessor;
import ro.racai.robin.nlp.TextProcessor.Query;
import ro.racai.robin.nlp.WordNet;

/**
 * @author Radu Ion ({@code radu@racai.ro})
 * <p>This is the main entry point for the ROBIN Dialogue manager.</p>
 */
public class RDManager {
	private RDUniverse discourseUniverse;
	private WordNet resouceWordNet;
	private Lexicon resourceLexicon;
	private TextProcessor resourceTextProc;
	private RDSayings resourceSayings;
	private String microworldName;
	
	/**
	 * @author Radu Ion ({@code radu@racai.ro})
	 * <p>This is the current state of the dialogue
	 * to be used by the client of this class.</p>
	 * 
	 */
	public static class DialogueState {
		/**
		 * What was the type of the previous query.
		 */
		QType previousQueryType;
		
		/**
		 * If this is not-null, the dialogue had ended
		 * and the robot behaviour is defined and 
		 * ready to be used by the client; 
		 */
		RDRobotBehaviour inferredBehaviour;
		
		/**
		 * This predicate was matched as being
		 * the closest one to the user's input
		 * from the universe of discourse.
		 */
		RDPredicate inferredPredicate;
		
		/**
		 * This field should be populated
		 * with the response of the robot: either requesting
		 * more information or providing the information for
		 * saying it. Each string is to be sent separately
		 * to the TTS engine.
		 */
		List<String> robotReply;
		
		public DialogueState() {
			inferredBehaviour = null;
			inferredPredicate = null;
			robotReply = new ArrayList<String>();
		}

		public boolean isDialogueDone() {
			return inferredBehaviour != null;
		}
		
		public List<String> getReply() {
			return robotReply;
		}
		
		public RDRobotBehaviour getBehaviour() {
			return inferredBehaviour;
		}
		
		/**
		 * <p>Canned response when the robot says fixed things.</p>
		 * @param qtyp       query type to set on the state;
		 * @return           a new {@link DialogueState} object.
		 */
		public static DialogueState robotSaysSomething(QType qtyp, List<String> lines) {
			DialogueState state = new DialogueState();
			
			state.robotReply = lines;
			state.previousQueryType = qtyp;
			return state;
		}
		
		/**
		 * <p>Canned response when the robot responds with
		 * the argument that checks the type of the query.</p>
		 * @param qtyp       query type; 
		 * @param pm         predicate match object;
		 * @return           a new {@link DialogueState} object with
		 *                   {@link RDRobotBehaviour} set.
		 */
		public static DialogueState robotInformedResponse(QType qtyp, PMatch pm) {
			DialogueState state = new DialogueState();
			
			state.inferredPredicate = pm.matchedPredicate;
			state.robotReply = new ArrayList<String>();
			state.robotReply.add(
					state.inferredPredicate.
					getArguments().
					get(pm.saidArgumentIndex).
					assignedReference
			);
			state.inferredBehaviour =
				new RDRobotBehaviour(
					state.inferredPredicate.getUserIntent(),
					state.inferredPredicate.
					getArguments().
					get(pm.saidArgumentIndex).
					assignedReference
				);
			
			state.previousQueryType = qtyp;
			return state;
		}
	}
	
	/**
	 * This is the current dialogue state,
	 * overwritten when a new dialogue starts
	 * as in "Salut Pepper!" or something similar.
	 */
	private DialogueState currentDState;
	
	public RDManager(WordNet wn, Lexicon lex, TextProcessor tproc, RDSayings say) {
		resouceWordNet = wn;
		resourceLexicon = lex;
		resourceTextProc = tproc;
		currentDState = new DialogueState();
		resourceSayings = say;
	}
	
	/**
	 * <p>Initialize the {@link #discourseUniverse} member field
	 * from the given {@code .mw} file.</p>
	 * @param mwFile
	 */
	public void loadMicroworld(String mwFile) {
		MWFileReader mwr = new MWFileReader(mwFile);
		
		discourseUniverse =
			mwr.constructUniverse(
				resouceWordNet,
				resourceLexicon,
				resourceTextProc
			);
		microworldName = mwr.getMicroworldName();
		// Set concepts on the text processor...
		resourceTextProc.setConceptList(discourseUniverse.getUniverseConcepts());
	}
	
	public String getMicroworldName() {
		return microworldName;
	}
	
	public String getConceptsAsString() {
		return
			String.join(
				System.lineSeparator(),
				discourseUniverse.getUniverseConcepts()
				.stream()
				.map(x -> x.toString())
				.collect(Collectors.toList())
			);
	}

	public String getPredicatesAsString() {
		return
			String.join(
				System.lineSeparator(),
				discourseUniverse.getUniversePredicates()
				.stream()
				.map(x -> x.toString())
				.collect(Collectors.toList())
			);
	}
	
	/**
	 * <p>This is the main method of the {@link RDManager}:
	 * it processes a textual user input are returns a
	 * {@link DialogueState} object.
	 * @param userInput      user input to operate with, comes
	 *                       from the ASR module;
	 * @return               a current state of the dialogue.
	 */
	public DialogueState doConversation(String userInput) {
		Query q =
			resourceTextProc.queryAnalyzer(
				resourceTextProc.textProcessor(userInput)
			);
		
		if (q.queryType == QType.HELLO) {
			currentDState =
				DialogueState.robotSaysSomething(
					q.queryType,
					resourceSayings.robotOpeningLines()
				);
					
			return currentDState;
		}

		if (q.queryType == QType.GOODBYE) {
			currentDState = null;
			
			return
				DialogueState.robotSaysSomething(
					q.queryType,
					resourceSayings.robotClosingLines()
				);
		}
		
		// 1. Try and match the query first...
		PMatch pm = discourseUniverse.resolveQuery(q);
		
		if (pm.matchedPredicate == null) {
			// No predicate found, this means no
			// predicate was found in KB. Return this
			// and say we do not know about it.
			currentDState =
				DialogueState.robotSaysSomething(
					q.queryType,
					resourceSayings.robotDontKnowLines()
				);
					
			return currentDState;
		}
		
		if (pm.saidArgumentIndex >= 0 && pm.isValidMatch) {
			// 2. Some predicate matched. If we have an
			// argument that we could return, that's
			// a success.
			currentDState = DialogueState.robotInformedResponse(q.queryType, pm);
		}
		else if (currentDState.inferredPredicate != null) {
			// 3. Some predicate matched but we don't have
			// enough information specified. Try to do a
			// match in the context of the previously
			// matched predicate.
			pm = discourseUniverse.resolveQueryInContext(q, currentDState.inferredPredicate);

			if (pm.saidArgumentIndex >= 0) {
				currentDState = DialogueState.robotInformedResponse(q.queryType, pm);
			}
			else {
				currentDState =
					DialogueState.robotSaysSomething(
						q.queryType,
						resourceSayings.robotDontKnowLines()
					);
			}
		}
		else {
			// No predicate found, this means no
			// predicate was found in KB. Return this
			// and say we do not know about it.
			currentDState =
				DialogueState.robotSaysSomething(
					q.queryType,
					resourceSayings.robotDontKnowLines()
				);
		}
		
		return currentDState;
	}

	/**
	 * Method to dump the resource caches so that
	 * we avoid expensive calls the text processing
	 * or resource querying web services. 
	 */
	public void dumpResourceCaches() {
		// Make sure you save expensive calls
		// to local hard disk...
		resourceTextProc.dumpTextCache();
		resouceWordNet.dumpWordNetCache();
	}
	
	private static String romanianDiacritics(String prompt) {
		prompt = prompt.replace("a^", "â");
		prompt = prompt.replace("i^", "î");
		prompt = prompt.replace("a@", "ă");
		prompt = prompt.replace("s@", "ș");
		prompt = prompt.replace("t@", "ț");
		prompt = prompt.replace("A^", "Â");
		prompt = prompt.replace("I^", "Î");
		prompt = prompt.replace("A@", "Ă");
		prompt = prompt.replace("S@", "Ș");
		prompt = prompt.replace("T@", "Ț");
		
		return prompt;
	}
	
	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		if (args.length != 1) {
			System.err.println("java ROBINDialog-1.0.0-SNAPSHOT-jar-with-dependencies.jar <.mw file>");
			return;
		}

		String mwFile = args[0];
		RoWordNet rown = new RoWordNet();
		RoLexicon rolex = new RoLexicon();
		RDSayings say = new RoSayings();
		RoTextProcessor rotp = new RoTextProcessor(rolex, rown, say);
		RDManager dman = new RDManager(rown, rolex, rotp, say);

		dman.loadMicroworld(mwFile);

		System.out.println("Default charset: " + Charset.defaultCharset().displayName());
		System.out.println();
		System.out.println("Use the following convention:");
		System.out.println("(replace lower with upper-case for upper-case diacritics)");
		System.out.println("  a^ for â");
		System.out.println("  i^ for î");
		System.out.println("  a@ for ă");
		System.out.println("  s@ for ș");
		System.out.println("  t@ for ț");
		System.out.println();
		System.out.println("Dialogue manager commands:");
		System.out.println("  'exit' or 'quit' to terminate this dialogue;");
		System.out.println("  'dump predicates' to print the list of ``known'' predicates in the KB;");
		System.out.println("  'dump concepts' to print the list of ``known'' bound concepts in the KB.");
		System.out.println();

		// A text-based dialogue loop.
		// Type 'exit' or 'quit' to end it.
		System.out.println("Running with the " + dman.getMicroworldName() + " microworld");
		System.out.print("User> ");

		Scanner scanner = new Scanner(System.in);
		String prompt = romanianDiacritics(scanner.nextLine());
		// String prompt = System.console().readLine();
		// BufferedReader scanner = new BufferedReader(new
		// InputStreamReader(System.in));
		// String prompt = scanner.readLine();

		while (
				!prompt.isEmpty() &&
				!prompt.equalsIgnoreCase("exit") &&
				!prompt.equalsIgnoreCase("quit")
		) {
			prompt = prompt.trim();
			
			if (prompt.startsWith("dump")) {
				String[] parts = prompt.split("\\s+");
				
				switch (parts[1]) {
				case "concepts":
					System.out.println(dman.getConceptsAsString());
					break;
				case "predicates":
					System.out.println(dman.getPredicatesAsString());
					break;
				default:
					System.out.println("Dialogue Manager> Unknown 'dump' command.");
				}
				
				System.out.print("User> ");
				prompt = romanianDiacritics(scanner.nextLine());
				continue;
			}

			DialogueState dstat = dman.doConversation(prompt);

			System.out.print("Pepper> ");
			System.out.println(String.join(" ", dstat.getReply()));

			if (dstat.isDialogueDone()) {
				System.out.print(dstat.getBehaviour());
			}

			System.out.print("User> ");
			prompt = romanianDiacritics(scanner.nextLine());
			// prompt = System.console().readLine();
			// prompt = scanner.readLine();
		} // end demo dialogue loop

		scanner.close();
		dman.dumpResourceCaches();
	}
}
