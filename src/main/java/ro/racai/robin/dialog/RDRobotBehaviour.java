/**
 * 
 */
package ro.racai.robin.dialog;

/**
 * @author Radu Ion ({@code radu@racai.ro})
 * <p>This is the object that ROBIN Dialogue Manager produces
 * for the actuating system of the robot (Pepper in our case). This
 * class only contains a {@link UIntentType} field, explaining what
 * the user wants and a ``payload'' String which means different things
 * as a function of {@link UIntentType}.</p>
 * <p>The <b>payload</b> is always a <i>reference</i> of a concept in
 * the {@link RDUniverse} in which the robot is operating and it has
 * a certain meaning to <i>the client of the ROBIN Dialogue Manager</i>
 * which is responsible with the creation of a meaningful {@link RDUniverse}.</p>
 * <p>For instance, in our familiar PRECIS scenario, Pepper's ``controllers'' could 
 * create a concept reference like e.g. <i>sala 209</i> which, for them, has a coordinate
 * on Pepper's internal map. For the ROBIN Dialogue Manager, these references and thus
 * the payload do not mean anything.</p>
 */
public class RDRobotBehaviour {
	/**
	 * User's intent type.
	 */
	private UIntentType userIntent;
	
	/**
	 * A string for Pepper to do something with it.
	 * If {@link #userIntent} is EXPLAIN_SOMETHING,
	 * Pepper (or the dialogue manager itself) will call
	 * the TTS system to say whatever is in the payload. 
	 */
	private String payload;
	
	/**
	 * <p>Default constructor, specifying user intention
	 * and payload.</p>
	 * @param uityp       user intention;
	 * @param pld         payload.
	 */
	public RDRobotBehaviour(UIntentType uityp, String pld) {
		userIntent = uityp;
		payload = pld;
	}
	
	public UIntentType getUserIntent() {
		return userIntent;
	}
	
	public String getPayload() {
		return payload;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return
			"User wants: " + userIntent.name() + System.lineSeparator() +
			"Extra info: " + payload + System.lineSeparator();
	}
}
