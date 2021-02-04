/**
 * 
 */
package ro.racai.robin.dialog;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import ro.racai.robin.dialog.RDPredicate.PMatch;
import ro.racai.robin.mw.MWFileReader;
import ro.racai.robin.nlp.Lexicon;
import ro.racai.robin.nlp.QType;
import ro.racai.robin.nlp.RoLexicon;
import ro.racai.robin.nlp.RoSpeechProcessing2;
import ro.racai.robin.nlp.RoTextProcessor;
import ro.racai.robin.nlp.RoWordNet;
import ro.racai.robin.nlp.SpeechProcessing;
import ro.racai.robin.nlp.TextProcessor;
import ro.racai.robin.nlp.TextProcessor.Query;
import ro.racai.robin.nlp.TextProcessor.Token;
import ro.racai.robin.nlp.WordNet;
import org.apache.log4j.Logger;

/**
 * @author Radu Ion ({@code radu@racai.ro})
 *         <p>
 *         This is the main entry point for the ROBIN Dialogue manager.
 *         </p>
 */
public class RDManager {
	private static final Logger LOG = Logger.getLogger(RDManager.class.getName());

	/**
	 * Set this to true to use ASR and TTS when calling the constructor. Else, use the standard
	 * input to type user input.
	 */
	private boolean confUseSpeech;
	private SpeechProcessing speechProcessor;
	private RDUniverse discourseUniverse;
	private WordNet resouceWordNet;
	private Lexicon resourceLexicon;
	private TextProcessor resourceTextProc;
	private RDSayings resourceSayings;
	private String microworldName;

	/**
	 * @author Radu Ion ({@code radu@racai.ro})
	 *         <p>
	 *         This is the current state of the dialogue to be used by the client of this class.
	 *         </p>
	 */
	public static class DialogueState {
		/**
		 * What was the type of the previous query.
		 */
		QType previousQueryType;

		/**
		 * If this is not-null, the dialogue had ended and the robot behaviour is defined and ready
		 * to be used by the client;
		 */
		RDRobotBehaviour inferredBehaviour;

		/**
		 * This predicate was matched as being the closest one to the user's input from the universe
		 * of discourse.
		 */
		RDPredicate inferredPredicate;

		/**
		 * This field should be populated with the response of the robot: either requesting more
		 * information or providing the information for saying it. Each string is to be sent
		 * separately to the TTS engine.
		 */
		List<String> robotReply;

		public DialogueState() {
			inferredBehaviour = null;
			inferredPredicate = null;
			robotReply = new ArrayList<>();
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
		 * <p>
		 * Canned response when the robot says fixed things.
		 * </p>
		 * 
		 * @param qtyp query type to set on the state;
		 * @return a new {@link DialogueState} object.
		 */
		public static DialogueState robotSaysSomething(QType qtyp, List<String> lines) {
			DialogueState state = new DialogueState();

			state.robotReply = lines;
			state.previousQueryType = qtyp;
			return state;
		}

		/**
		 * <p>
		 * Canned response when the robot responds with the argument that checks the type of the
		 * query.
		 * </p>
		 * 
		 * @param qtyp query type;
		 * @param pm   predicate match object;
		 * @return a new {@link DialogueState} object with {@link RDRobotBehaviour} set.
		 */
		public static DialogueState robotInformedResponse(QType qtyp, PMatch pm) {
			DialogueState state = new DialogueState();

			state.inferredPredicate = pm.matchedPredicate;
			state.robotReply = new ArrayList<>();
			state.robotReply.add(state.inferredPredicate.getArguments().get(pm.saidArgumentIndex)
					.getReference());
			state.inferredBehaviour = new RDRobotBehaviour(state.inferredPredicate.getUserIntent(),
					state.inferredPredicate.getArguments().get(pm.saidArgumentIndex)
							.getReference());

			state.previousQueryType = qtyp;
			return state;
		}
	}

	/**
	 * This is the current dialogue state, overwritten when a new dialogue starts as in "Salut
	 * Pepper!" or something similar.
	 */
	private DialogueState currentDState;

	public RDManager(WordNet wn, Lexicon lex, TextProcessor tproc, RDSayings say,
			SpeechProcessing sproc, boolean speech) {
		resouceWordNet = wn;
		resourceLexicon = lex;
		resourceTextProc = tproc;
		currentDState = new DialogueState();
		resourceSayings = say;
		confUseSpeech = speech;
		speechProcessor = sproc;
	}

	/**
	 * <p>
	 * Initialize the {@link #discourseUniverse} member field from the given {@code .mw} file.
	 * </p>
	 * 
	 * @param mwFile
	 */
	public void loadMicroworld(String mwFile) {
		MWFileReader mwr = new MWFileReader(mwFile);

		discourseUniverse =
				mwr.constructUniverse(resouceWordNet, resourceLexicon, resourceTextProc);
		microworldName = mwr.getMicroworldName();
		// Set DICT ASR correction rules on the text processor
		resourceTextProc.setASRDictionary(discourseUniverse.getASRRulesMap());
	}

	public String getMicroworldName() {
		return microworldName;
	}

	public String getConceptsAsString() {
		return String.join(System.lineSeparator(), discourseUniverse.getBoundConcepts().stream()
				.map(RDConcept::toString).collect(Collectors.toList()));
	}

	public String getPredicatesAsString() {
		return String.join(System.lineSeparator(), discourseUniverse.getBoundPredicates().stream()
				.map(RDPredicate::toString).collect(Collectors.toList()));
	}

	/**
	 * <p>
	 * This is the main method of the {@link RDManager}: it processes a textual user input are
	 * returns a {@link DialogueState} object.
	 * 
	 * @param userProcessedInput user processed input to operate with, comes from the ASR module;
	 *                           <b>it is assumed to be correct!</b>
	 * @return a current state of the dialogue.
	 */
	public DialogueState doConversation(List<Token> userProcessedInput) {
		Query q = resourceTextProc.queryAnalyzer(userProcessedInput,
				discourseUniverse.getDefinedConcepts());

		if (q == null) {
			// 1. No predicate found, this means no predicate was found in KB. Return this and say
			// we do not know about it.
			currentDState = DialogueState.robotSaysSomething(QType.UNKNOWN,
					resourceSayings.robotDontKnowLines());
			return currentDState;
		}

		// 2. Hello or Goodbye
		if (q.queryType == QType.HELLO) {
			currentDState = DialogueState.robotSaysSomething(q.queryType,
					resourceSayings.robotOpeningLines());

			return currentDState;
		}

		if (q.queryType == QType.GOODBYE) {
			currentDState = null;

			return DialogueState.robotSaysSomething(q.queryType,
					resourceSayings.robotClosingLines());
		}

		// 3. Match the query first...
		PMatch pm = discourseUniverse.resolveQuery(q);

		if (pm == null || pm.matchedPredicate == null) {
			// 4. No predicate found, this means no predicate was found in KB. Return this and say
			// we
			// do not know about it.
			currentDState = DialogueState.robotSaysSomething(q.queryType,
					resourceSayings.robotDontKnowLines());

			return currentDState;
		}

		if (pm.isFullMatch() && !pm.containsJavaReference() && !pm.hasUnresolvedVariable) {
			// 5. Some predicate from KB matched fully. Just answer "Yes."
			currentDState =
					DialogueState.robotSaysSomething(q.queryType, resourceSayings.robotSayYes());

			return currentDState;
		} else if (pm.saidArgumentIndex >= 0 && pm.isValidMatch) {
			// 3. Some predicate matched. If we have an
			// argument that we could return, that's
			// a success.
			currentDState = DialogueState.robotInformedResponse(q.queryType, pm);
		} else if (currentDState.inferredPredicate != null) {
			// 4. Some predicate matched but we don't have
			// enough information specified. Try to do a
			// match in the context of the previously
			// matched predicate.
			pm = discourseUniverse.resolveQueryInContext(q, currentDState.inferredPredicate);

			if (pm.saidArgumentIndex >= 0) {
				currentDState = DialogueState.robotInformedResponse(q.queryType, pm);
			} else {
				currentDState = DialogueState.robotSaysSomething(q.queryType,
						resourceSayings.robotDontKnowLines());
			}
		} else {
			// 5. No predicate found, this means no
			// predicate was found in KB. Return this
			// and say we do not know about it.
			currentDState = DialogueState.robotSaysSomething(q.queryType,
					resourceSayings.robotDontKnowLines());
		}

		return currentDState;
	}

	/**
	 * Method to dump the resource caches so that we avoid expensive calls the text processing or
	 * resource querying web services.
	 */
	public void dumpResourceCaches() {
		// Make sure you save expensive calls
		// to local hard disk...
		resourceTextProc.dumpTextCache();
		resouceWordNet.dumpWordNetCache();
	}

	private String getUserInput() {
		if (confUseSpeech) {
			return speechProcessor.speechToText();
		} else {
			System.out.print("Text input> ");
			System.out.flush();
			// Could not make UTF-8 console read work in Windows 10...
			String input = System.console().readLine();

			input = input.replace("i^", "î");
			input = input.replace("a^", "â");
			input = input.replace("a@", "ă");
			input = input.replace("s~", "ș");
			input = input.replace("t~", "ț");
			input = input.replace("I^", "Î");
			input = input.replace("A^", "Â");
			input = input.replace("A@", "Ă");
			input = input.replace("S~", "Ș");
			input = input.replace("T~", "Ț");

			return input;
		}
	}

	/**
	 * Convenience method to output the reply and say it if it is such desired.
	 * 
	 * @param replies the reply lines from the manager.
	 * @throws IOException
	 * @throws LineUnavailableException
	 * @throws UnsupportedAudioFileException
	 * @throws InterruptedException
	 */
	private void produceOutput(List<String> replies) throws IOException, LineUnavailableException,
			UnsupportedAudioFileException, InterruptedException {
		
		for (int i = 0; i < replies.size(); i++) {
			String pepperTalk = resourceTextProc.expandEntities(replies.get(i));

			System.out.println("Pepper> " + pepperTalk);

			if (confUseSpeech) {
				speechProcessor.playUtterance(speechProcessor.textToSpeech(pepperTalk));
			}
		}
	}

	private static String getROBINDialogVersion() {
		try {
			File pom = new File("pom.xml");

			if (pom.exists()) {
				MavenXpp3Reader reader = new MavenXpp3Reader();
				Model model = reader.read(new FileReader(pom));

				return model.getVersion();
			} else {
				// TODO: read version information from jar
			}
		} catch (IOException ioe) {
			ioe.printStackTrace();
		} catch (XmlPullParserException xppe) {
			xppe.printStackTrace();
		}

		return "?.?.?-SNAPSHOT";
	}

	/**
	 * Call this method to instantiate an {@link RDManager} object.
	 * 
	 * @return an {@link RDManager} application (Java actionable object).
	 */
	public static RDManager createApplication(String mwFile) {
		String version = getROBINDialogVersion();

		LOG.info(String.format("ROBINDialog version %s", version));

		RoSpeechProcessing2 speech = new RoSpeechProcessing2();
		RoWordNet rown = new RoWordNet();
		RoLexicon rolex = new RoLexicon();
		RoSayings say = new RoSayings();
		RoTextProcessor rotp = new RoTextProcessor(rolex, rown, say);
		RDManager dman = new RDManager(rown, rolex, rotp, say, speech, false);

		dman.loadMicroworld(mwFile);

		LOG.info(String.format("Running with the %s microworld", dman.getMicroworldName()));

		return dman;
	}

	public List<Token> processPrompt(String prompt) {
		return resourceTextProc.textProcessor(prompt, false, false);
	}

	/**
	 * @param args
	 * @throws IOException
	 * @throws UnsupportedAudioFileException
	 * @throws LineUnavailableException
	 * @throws InterruptedException
	 */
	public static void main(String[] args) throws IOException, LineUnavailableException,
			UnsupportedAudioFileException, InterruptedException {
		if (args.length != 1) {
			String version = getROBINDialogVersion();
			System.err.println(
					"java ROBINDialog-" + version + "-jar-with-dependencies.jar <.mw file>");
			return;
		}

		String mwFile = args[0];
		RDManager dman = createApplication(mwFile);
		String prompt = dman.getUserInput();

		// A text-based dialogue loop with speech/console input and output.
		while (!prompt.isEmpty()
				&& !dman.resourceSayings.userClosingStatement(Arrays.asList(prompt.split("\\s+")))) {
			List<Token> pprompt = dman.processPrompt(prompt);

			System.out.println("User> " + dman.resourceTextProc.toPromptString(pprompt));

			DialogueState dstat = dman.doConversation(pprompt);

			dman.produceOutput(dstat.getReply());

			if (dstat.isDialogueDone()) {
				System.out.print(dstat.getBehaviour());
			}

			prompt = dman.getUserInput();
		} // end demo dialogue loop

		System.out.println("Pepper> La revedere.");
		dman.dumpResourceCaches();
	}
}
