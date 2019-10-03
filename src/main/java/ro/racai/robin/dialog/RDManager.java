/**
 * 
 */
package ro.racai.robin.dialog;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

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
	
	/**
	 * @author Radu Ion ({@code radu@racai.ro})
	 * <p>This is the current state of the dialogue
	 * to be used by the client of this class.</p>
	 * 
	 */
	public static class DialogueState {
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
	}
	
	public String getMicroworldName() {
		return discourseUniverse.toString();
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
			currentDState = new DialogueState();
			currentDState.robotReply = resourceSayings.robotOpeningLines();
			return currentDState;
		}

		if (q.queryType == QType.GOODBYE) {
			currentDState = new DialogueState();
			currentDState.robotReply = resourceSayings.robotClosingLines();
			return currentDState;
		}
		
		if (currentDState == null) {
			currentDState = new DialogueState();
			
			PMatch pm = discourseUniverse.resolveQuery(q);
			
			if (pm.matchedPredicate == null) {
				currentDState.robotReply = resourceSayings.robotDidntUnderstandLines();
				return currentDState;
			}
			else {
				currentDState.inferredPredicate = pm.matchedPredicate;
				
				if (pm.saidArgumentIndex >= 0) {
					currentDState.robotReply = new ArrayList<String>();
					currentDState.robotReply.add(
						currentDState.inferredPredicate.
							getArguments().
							get(pm.saidArgumentIndex).
							assignedReference
					);
					currentDState.inferredBehaviour =
						new RDRobotBehaviour(
							currentDState.inferredPredicate.getUserIntent(),
							currentDState.inferredPredicate.
								getArguments().
								get(pm.saidArgumentIndex).
								assignedReference
						);
					
					return currentDState;
				}
				else {
					// TODO: Not implemented yet, ask further questions...
					return null;
				}
			}
		}
		else {
			// TODO: Not implemented yet, ask further questions...
			return null;
		}
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
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		if (args.length != 1) {
			System.err.println("java ");
		}
		
		RoWordNet rown = new RoWordNet();
		RoLexicon rolex = new RoLexicon();
		RDSayings say = new RoSayings();
		RoTextProcessor rotp = new RoTextProcessor(rolex, say);
		RDManager dman = new RDManager(rown, rolex, rotp, say);
		
		dman.loadMicroworld("src/main/resources/precis.mw");
		
		// A text-based dialogue loop.
		// Type 'exit' or 'quit' to end it.
		System.out.println("Running with MW " + dman.getMicroworldName());
		System.out.print("User> ");
        Scanner scanner = new Scanner(System.in);
        String prompt = scanner.nextLine();
        
        while (
        	!prompt.isEmpty() &&
        	!prompt.equalsIgnoreCase("exit") &&
        	!prompt.equalsIgnoreCase("quit")
        ) {
        	prompt = prompt.trim();
        	
        	DialogueState dstat = dman.doConversation(prompt);
        
        	System.out.print("Pepper> ");
        	System.out.println(String.join(" ", dstat.getReply()));
        	
        	if (dstat.isDialogueDone()) {
        		System.out.print(dstat.getBehaviour());
        	}
        	
        	prompt = scanner.nextLine();
        } // end demo dialogue loop
        
        scanner.close();
		dman.dumpResourceCaches();
	}
}
