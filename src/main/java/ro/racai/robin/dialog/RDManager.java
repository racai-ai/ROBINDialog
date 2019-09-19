/**
 * 
 */
package ro.racai.robin.dialog;

import ro.racai.robin.mw.MWFileReader;
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
		RoTextProcessor tp = new RoTextProcessor();
		MWFileReader mwr = new MWFileReader("src/main/resources/precis.mw");
		RoWordNet rown = new RoWordNet();
		RDUniverse precis = mwr.constructUniverse(rown);
		Query q = tp.queryAnalyzer(tp.textProcessor("În ce sală se desfășoară cursul de sisteme de operare?"));
		
		precis.resolveQuery(q);
		
		return;
	}
}
