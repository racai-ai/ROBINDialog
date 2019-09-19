/**
 * 
 */
package ro.racai.robin.mw;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;

import ro.racai.robin.dialog.CType;
import ro.racai.robin.dialog.RDConcept;
import ro.racai.robin.dialog.RDConstant;
import ro.racai.robin.dialog.RDPredicate;
import ro.racai.robin.dialog.RDUniverse;
import ro.racai.robin.dialog.UIntentType;
import ro.racai.robin.nlp.RoWordNet;
import ro.racai.robin.nlp.WordNet;

/**
 * @author Radu Ion ({@code radu@racai.ro})
 * <p>Builds an {@link RDUniverse} from a text {@code .mw} file.
 * For an example file, please check the {@code src/main/resources/precis.mw} file.
 * Instructions on how to create a new micro-world are in this file, as comments.</p>
 */
public class MWFileReader implements RDMicroworld {
	private static final Logger LOGGER =
		Logger.getLogger(MWFileReader.class.getName());	
	private String mwFilePath;
	// CONCEPT sală, laborator, cameră -> LOCATION
	private final static Pattern CONCEPT_PATT =
		Pattern.compile("^CONCEPT\\s+(.+)\\s*->\\s*([A-Z_]+)$");
	// REFERENCE curs laboratorul de informatică = C1
	private final static Pattern REFERENCE_PATT =
		Pattern.compile("^REFERENCE\\s+([^ \\t]+)\\s+([^=]+?)\\s*=\\s*([a-zA-Z0-9]+)$");
	// TIME marți, 8:00 = T1
	// PERSON Adriana Vlad = P1
	private final static Pattern CTYPE_PATT =
		Pattern.compile("^" + CType.getMemberRegex() + "\\s+([^=]+?)\\s*=\\s*([a-zA-Z0-9]+)$");
	// PREDICATE ține, desfășura -> EXPLAIN_SOMETHING
	private final static Pattern PREDICATE_PATT =
		Pattern.compile("^PREDICATE\\s+(.+)\\s*->\\s*([A-Z_]+)$");
	
	
	/**
	 * <p>Constructs a {@code .mw} file reader from a given file.</p>
	 * @param file      the file containing the micro-world definition.
	 */
	public MWFileReader(String file) {
		mwFilePath = file;
	}
	
	/* (non-Javadoc)
	 * @see ro.racai.robin.mw.RDMicroworld#constructUniverse()
	 */
	@Override
	public RDUniverse constructUniverse(WordNet wn) {
		try {
			BufferedReader rdr =
				new BufferedReader(
					new InputStreamReader(
						new FileInputStream(mwFilePath), "UTF8"));
			String line = rdr.readLine();
			List<RDConcept> definedConcepts =
				new ArrayList<RDConcept>();
			List<RDPredicate> definedPredicates =
				new ArrayList<RDPredicate>();
			Map<String, RDConcept> referencedConcepts =
				new HashMap<String, RDConcept>();
			List<RDPredicate> truePredicates =
				new ArrayList<RDPredicate>();
			int lineCount = 1;
			
			while (line != null) {
				line = line.trim();
				
				if (line.startsWith("#")) {
					// Skip comment lines
					line = rdr.readLine();
					lineCount++;
					continue;
				} // End comment
				
				if (
					line.startsWith("CONCEPT ") ||
					line.startsWith("CONCEPT\t")
				) {
					Matcher cm = CONCEPT_PATT.matcher(line);
					
					if (cm.find()) {
						String csyn = cm.group(1);
						String ctyp = cm.group(2);
						CType conceptType = null;
						
						try {
							conceptType = CType.valueOf(ctyp);
						}
						catch (IllegalArgumentException iae) {
							LOGGER.error(
								"'" + ctyp + "' " +
								"is not a recognized ro.racai.robin.dialog.CType " +
								"member at line " + lineCount + "!"
							);
							iae.printStackTrace();
							rdr.close();
							return null;
						}
						
						if (csyn.contains(",")) {
							List<String> synParts =
								new ArrayList<String>(Arrays.asList(csyn.split(",\\s*")));
							String canonName = synParts.remove(0);
							
							definedConcepts.add(
								RDConcept.Builder(
									conceptType, canonName, synParts, null)
								);
						}
						else {
							definedConcepts.add(
								RDConcept.Builder(
									conceptType, csyn, new ArrayList<String>(), null)
								);
						}
					}
					else {
						LOGGER.warn("CONCEPT line is not well-formed at line " + lineCount + "...");
					}
				} // end CONCEPT keyword
				else if (
					line.startsWith("REFERENCE ") ||
					line.startsWith("REFERENCE\t")
				) {
					if (definedConcepts.isEmpty()) {
						LOGGER.error(
							"Found references for missing concepts. " +
							"Please start with CONCEPT definitions.");
						rdr.close();
						return null;
					}
					
					Matcher rm = REFERENCE_PATT.matcher(line);
					
					if (rm.find()) {
						String canonName = rm.group(1);
						String reference = rm.group(2);
						String refCode = rm.group(3);
						boolean conceptFound = false;
						
						for (RDConcept c : definedConcepts) {
							if (c.getCanonicalName().equals(canonName)) {
								RDConcept nc = c.DeepCopy();
								
								nc.setReference(reference);
								
								if (!referencedConcepts.containsKey(refCode)) {
									referencedConcepts.put(refCode, nc);
									conceptFound = true;
									break;
								}
								else {
									LOGGER.error(
										"Reference code '" + refCode +
										"' is duplicated at line " + lineCount + "!");
									rdr.close();
									return null;
								}
							}
						}
						
						if (!conceptFound) {
							LOGGER.error(
								"Found reference for a missing concept '" + canonName +
								"' at line " + lineCount + ". " +
								"Please add a CONCEPT definition above.");
							rdr.close();
							return null;
						}
					}
					else {
						LOGGER.warn("REFERENCE line is not well-formed at line " + lineCount + "...");
					}
				} // end REFERENCE keyword
				else if (CTYPE_PATT.matcher(line).find()) {
					Matcher ctm = CTYPE_PATT.matcher(line);
					
					ctm.find();
					
					CType constType = CType.valueOf(ctm.group(1));
					String constValue = ctm.group(2);
					RDConstant constant = new RDConstant(constType, constValue);
					String constCode = ctm.group(3);
					
					if (!referencedConcepts.containsKey(constCode)) {
						referencedConcepts.put(constCode, constant);
					}
					else {
						LOGGER.error(
							"Constant concept code '" + constCode +
							"' is duplicated at line " + lineCount + "!");
						rdr.close();
						return null;
					}
				} // end CType keyword
				else if (
					line.startsWith("PREDICATE ") ||
					line.startsWith("PREDICATE\t")
				) {
					Matcher pm = PREDICATE_PATT.matcher(line);
					
					if (pm.find()) {
						String psyn = pm.group(1);
						String usri = pm.group(2);
						UIntentType userIntent = null;
						
						try {
							userIntent = UIntentType.valueOf(usri);
						}
						catch (IllegalArgumentException iae) {
							LOGGER.error(
								"'" + usri + "' " +
								"is not a recognized ro.racai.robin.dialog.UIntentType " +
								"member at line " + lineCount + "!"
							);
							iae.printStackTrace();
							rdr.close();
							return null;
						}
						
						if (psyn.contains(",")) {
							List<String> synParts =
								new ArrayList<String>(Arrays.asList(psyn.split(",\\s*")));
							String canonName = synParts.remove(0);
							
							definedPredicates.add(
								RDPredicate.Builder(
									userIntent, canonName, synParts, null)
								);
						}
						else {
							definedPredicates.add(
								RDPredicate.Builder(
									userIntent, psyn, new ArrayList<String>(), null)
								);
						}
					}
					else {
						LOGGER.warn("PREDICATE line is not well-formed at line " + lineCount + "...");
					}					
				}// end PREDICATE keyword
				else if (
					line.startsWith("TRUE ") ||
					line.startsWith("TRUE\t")
				) {
					String[] trueParts = line.split("\\s+");
					String actionVerb = trueParts[1];
					boolean predicateFound = false;
					
					for (RDPredicate p : definedPredicates) {
						if (p.getActionVerb().equalsIgnoreCase(actionVerb)) {
							RDPredicate np = p.DeepCopy();
							
							// Add arguments to the predicate
							for (int i = 2; i < trueParts.length; i++) {
								String refCode = trueParts[i];
								
								if (referencedConcepts.containsKey(refCode)) {
									np.addArgument(referencedConcepts.get(refCode));
								}
								else {
									LOGGER.error(
										"Reference code '" + refCode +
										"' was not declared before at " + lineCount + "!");
									rdr.close();
									return null;
								}
							} // end all arguments
							
							predicateFound = true;
							truePredicates.add(np);
							break;
						} // end predicate found
					} // end all defined predicates
					
					if (!predicateFound) {
						LOGGER.error(
							"Predicate '" + actionVerb +
							"' was not declared before with a PREDICATE line, at " + lineCount + "!");
							rdr.close();
							return null;
					}
				} // end TRUE keyword
				
				line = rdr.readLine();
				lineCount++;
			} // end all .mw file
		
			rdr.close();
			
			RDUniverse universe = new RDUniverse(wn);
			
			universe.addPredicates(truePredicates);
			
			for (String ref : referencedConcepts.keySet()) {
				universe.addConcept(referencedConcepts.get(ref));
			}
			
			return universe;
		}
		catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}

	@Override
	public String getMicroworldName() {
		File mwFile = new File(mwFilePath);
		String fileName = mwFile.getName();
		
		fileName = fileName.replaceFirst("\\.mw$", "");
		fileName = fileName.toUpperCase();
		
		return fileName;
	}
}
