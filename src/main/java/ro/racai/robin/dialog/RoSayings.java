/**
 * 
 */
package ro.racai.robin.dialog;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Radu Ion ({@code radu@racai.ro})
 *         <p>
 *         Romanian version.
 *         </p>
 */
public class RoSayings implements RDSayings {
	private static final Set<String> OPENING_LINES = new HashSet<>();
	private static final Set<String> CLOSING_LINES = new HashSet<>();

	static {
		// Everything is lower-cased here!
		CLOSING_LINES.add("mulțumesc");
		CLOSING_LINES.add("mulțam");
		CLOSING_LINES.add("mersi");
		CLOSING_LINES.add("pa");
		CLOSING_LINES.add("la revedere");

		// Everything is lower-cased here!
		for (String roboName : Arrays.asList("", "pepper", "pepăr", "robotule", "robot", "roboțel",
				"roboțelul", "roboțelule")) {
			if (!roboName.isEmpty()) {
				OPENING_LINES.add(roboName);
				OPENING_LINES.add("salut " + roboName);
				OPENING_LINES.add("servus " + roboName);
				OPENING_LINES.add("noroc " + roboName);
				OPENING_LINES.add("bună " + roboName);
				OPENING_LINES.add("bună ziua " + roboName);
				OPENING_LINES.add("bună seara " + roboName);
				OPENING_LINES.add("bună dimineața " + roboName);
				OPENING_LINES.add("buna " + roboName);
				OPENING_LINES.add("buna ziua " + roboName);
				OPENING_LINES.add("buna seara " + roboName);
				OPENING_LINES.add("buna dimineața " + roboName);
				OPENING_LINES.add("neața " + roboName);
			} else {
				OPENING_LINES.add("salut");
				OPENING_LINES.add("servus");
				OPENING_LINES.add("noroc");
				OPENING_LINES.add("bună");
				OPENING_LINES.add("bună ziua");
				OPENING_LINES.add("bună seara");
				OPENING_LINES.add("bună dimineața");
				OPENING_LINES.add("buna");
				OPENING_LINES.add("buna ziua");
				OPENING_LINES.add("buna seara");
				OPENING_LINES.add("buna dimineața");
				OPENING_LINES.add("neața");
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see ro.racai.robin.dialog.RDSayings#userOpeningStatement(java.util.List)
	 */
	@Override
	public boolean userOpeningStatement(List<String> words) {
		String lcExpression = String.join(" ", words.stream()
				// Skip punctuation...
				.filter(x -> !x.matches("^\\W+$")).map(x -> x.trim().toLowerCase())
				.collect(Collectors.toList()));

		return OPENING_LINES.contains(lcExpression);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see ro.racai.robin.dialog.RDSayings#userClosingStatement(java.util.List)
	 */
	@Override
	public boolean userClosingStatement(List<String> words) {
		String lastWord = words.get(words.size() - 1);

		// The text corrector adds final punctuation.
		// Remove it so that we can match the phrase.
		if (lastWord.matches("^.+[.?!]$")) {
			words.set(words.size() - 1, lastWord.substring(0, lastWord.length() - 1));
		}

		String lcExpression = String.join(" ", words.stream().filter(x -> !x.matches("^\\W+$"))
				.map(x -> x.trim().toLowerCase()).collect(Collectors.toList()));

		return CLOSING_LINES.contains(lcExpression);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see ro.racai.robin.dialog.RDSayings#robotOpeningLines()
	 */
	@Override
	public List<String> robotOpeningLines() {
		List<String> lines = new ArrayList<>();
		int hourNow = LocalDateTime.now().getHour();

		if (hourNow >= 0 && hourNow < 10) {
			lines.add("Bună dimineața.");
		}
		else if (hourNow >= 10 && hourNow < 19) {
			lines.add("Bună ziua.");
		}
		else {
			lines.add("Bună seara.");
		}

		return lines;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see ro.racai.robin.dialog.RDSayings#robotClosingLines()
	 */
	@Override
	public List<String> robotClosingLines() {
		List<String> lines = new ArrayList<>();

		lines.add("La revedere.");

		return lines;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see ro.racai.robin.dialog.RDSayings#robotDontKnowLines()
	 */
	@Override
	public List<String> robotDontKnowLines() {
		List<String> lines = new ArrayList<>();

		lines.add("Această informație nu îmi este disponibilă.");

		return lines;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see ro.racai.robin.dialog.RDSayings#robotDidntUnderstandLines()
	 */
	@Override
	public List<String> robotDidntUnderstandLines() {
		List<String> lines = new ArrayList<>();

		lines.add("Vă rog să reformulați.");

		return lines;
	}
}
