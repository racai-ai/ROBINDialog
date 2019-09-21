/**
 * 
 */
package ro.racai.robin.dialog;

import ro.racai.robin.mw.MWFileReader;
import ro.racai.robin.nlp.RoLexicon;
import ro.racai.robin.nlp.RoTextProcessor;
import ro.racai.robin.nlp.RoWordNet;
import ro.racai.robin.nlp.TextProcessor.Query;

/**
 * @author Radu Ion ({@code radu@racai.ro})
 * <p>This is the main entry point for the ROBIN Dialogue manager.</p>
 */
public class RDManager {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		RoWordNet rown = new RoWordNet();
		RoLexicon rolex = new RoLexicon();
		RoTextProcessor rotp = new RoTextProcessor(rolex);
		MWFileReader mwr = new MWFileReader("src/main/resources/precis.mw");
		RDUniverse precis = mwr.constructUniverse(rown, rolex, rotp);
		Query q =
			rotp.queryAnalyzer(
				rotp.textProcessor(
					"În ce sală se desfășoară cursul de sisteme de operare?"
				)
			);
		
		RDPredicate pred = precis.resolveQuery(q);
		
		System.out.println(pred);
		// Make sure you save expensive calls
		// to local hard disk...
		rotp.dumpTextCache();
		rown.dumpWordNetCache();
	}
}
