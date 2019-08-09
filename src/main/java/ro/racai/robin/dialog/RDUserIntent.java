/**
 * 
 */
package ro.racai.robin.dialog;

import java.util.List;

/**
 * @author Radu Ion ({@code radu@racai.ro})
 * <p>This represents a user intent and the {@link RDManager} maintains
 * a probability distribution over these objects.</p>
 */
public abstract class RDUserIntent {
	
	protected UIntentType intentType;
	
	/**
	 * This represents the probability that the dialogue
	 * manager assigns to this object as to it being true
	 * (user wants this) or not.
	 */
	protected float probabilityOfBeingWanted;
		
	/**
	 * Set this to the concept that "will give away"
	 * this user intent. For example, in our PRECIS scenario,
	 * this would be the <i>sală</i> concept
	 */
	protected RDConcept conceptHint;
	
	/**
	 * Set this list to lemmas of verbs "giving away"
	 * this user intent. For example, in our PRECIS scenario,
	 * if the user wants to know where the {@link #conceptHint} is,
	 * set this list to e.g. "fi", "exista".
	 */
	protected List<String> actionVerbs;
	
	public void addActionVerb(String lemma) {
		actionVerbs.add(lemma.trim().toLowerCase());
	}
	
	public void setConcept(RDConcept concept) {
		conceptHint = concept;
	}
}
