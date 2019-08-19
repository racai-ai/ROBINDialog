/**
 * 
 */
package ro.racai.robin.nlp;

import java.util.List;

/**
 * @author Radu Ion ({@code radu@racai.ro})
 * <p>Romanian action verbs to be used in ROBIN Dialog.</p>
 */
public class RoLexicon implements Lexicon {
	/* (non-Javadoc)
	 * @see ro.racai.robin.nlp.Lexicon#isCommandVerb(java.lang.String)
	 */
	@Override
	public boolean isCommandVerb(String verbLemma) {
		return
			verbLemma.equalsIgnoreCase("duce") ||
			verbLemma.equalsIgnoreCase("conduce") ||
			verbLemma.equalsIgnoreCase("arăta") ||
			verbLemma.equalsIgnoreCase("aduce");
	}

	@Override
	public boolean isFunctionalPOS(String pos) {
		return !pos.matches("^(N|P[^x]|M|R[gw]|Vm|Af|Y).*$");
	}

	@Override
	public boolean isNounPOS(String pos) {
		// Some extensions for Romanian, to accommodate
		// words such as "unde" and "când"
		return pos.matches("^(N|P[^x]|M|Rw|Yn?).*$");
	}

	@Override
	public boolean isClosingStatement(List<String> words) {
		if (
			words.size() == 1 &&
			(
				words.get(0).trim().equalsIgnoreCase("mulțumesc") ||
				words.get(0).trim().equalsIgnoreCase("mulțam") ||
				words.get(0).trim().equalsIgnoreCase("mersi") ||
				words.get(0).trim().equalsIgnoreCase("pa")
			)
		) {
			return true;
		}
		
		if (
			words.size() == 2 &&
			(
				words.get(0).trim().equalsIgnoreCase("la") &&
				words.get(1).trim().equalsIgnoreCase("revedere")
			)
		) {
			return true;
		}
		
		return false;
	}

	@Override
	public boolean isPrepositionPOS(String pos) {
		return pos.startsWith("Sp");
	}
}
