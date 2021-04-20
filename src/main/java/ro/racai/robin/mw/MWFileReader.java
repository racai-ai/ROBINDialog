/**
 * 
 */
package ro.racai.robin.mw;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
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
import ro.racai.robin.nlp.Lexicon;
import ro.racai.robin.nlp.TextProcessor;
import ro.racai.robin.nlp.WordNet;

/**
 * @author Radu Ion ({@code radu@racai.ro})
 *         <p>
 *         Builds an {@link RDUniverse} from a text {@code .mw} file. For an example file, please
 *         check the {@code src/main/resources/precis.mw} file. Instructions on how to create a new
 *         micro-world are in this file, as comments.
 *         </p>
 */
public class MWFileReader implements RDMicroworld {
	private static final Logger LOGGER = Logger.getLogger(MWFileReader.class.getName());
	private String mwFilePath;
	private static final String COMMA_RX_STR = ",\\s*";
	private static final Pattern DICT_PATT = Pattern.compile("^DICT\\s+\"(.+?)\"\\s+(.+)$");
	private static final Pattern DICT_PATT2 = Pattern.compile("^DICT\\s+(.+)\\s+(.+)$");
	// CONCEPT sală, laborator, cameră -> LOCATION
	private static final Pattern CONCEPT_PATT =
			Pattern.compile("^CONCEPT\\s+(.+)\\s*->\\s*([A-Za-zșțăîâȘȚĂÎÂ_-]+)$");
	// REFERENCE curs laboratorul de informatică = C1
	private static final Pattern REFERENCE_PATT =
			Pattern.compile("^REFERENCE\\s+([^ \\t]+)\\s+([^=]+?)\\s*=\\s*([a-zA-Z0-9]+)$");
	// TIME marți, 8:00 = T1
	// PERSON Adriana Vlad = P1
	private static final Pattern CTYPE_PATT =
			Pattern.compile("^" + CType.getMemberRegex() + "\\s+([^=]+?)\\s*=\\s*([a-zA-Z0-9]+)$");
	// PREDICATE ține, desfășura -> EXPLAIN_SOMETHING
	private static final Pattern PREDICATE_PATT =
			Pattern.compile("^PREDICATE\\s+(.+)\\s*->\\s*([A-Z_]+)$");

	/**
	 * <p>
	 * Constructs a {@code .mw} file reader from a given file.
	 * </p>
	 * 
	 * @param file the file containing the micro-world definition.
	 */
	public MWFileReader(String file) {
		mwFilePath = file;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see ro.racai.robin.mw.RDMicroworld#constructUniverse()
	 */
	@Override
	public RDUniverse constructUniverse(WordNet wn, Lexicon lex, TextProcessor proc) {
		try (BufferedReader rdr = new BufferedReader(
				new InputStreamReader(new FileInputStream(mwFilePath), StandardCharsets.UTF_8))) {
			String line = rdr.readLine();
			Map<String, String> asrDictionary = new HashMap<>();
			List<RDConcept> definedConcepts = new ArrayList<>();
			List<RDPredicate> definedPredicates = new ArrayList<>();
			Map<String, RDConcept> referencedConcepts = new HashMap<>();
			List<RDPredicate> truePredicates = new ArrayList<>();
			int lineCount = 1;

			while (line != null) {
				line = line.trim();

				if (line.startsWith("#")) {
					// Skip comment lines
					line = rdr.readLine();
					lineCount++;
					continue;
				} // End comment

				if (line.startsWith("DICT ") || line.startsWith("DICT\t")) {
					Matcher dm = DICT_PATT.matcher(line);

					if (dm.find()) {
						String asrPhr = dm.group(1);
						String crtPhr = dm.group(2);

						asrDictionary.put(asrPhr.trim(), crtPhr.trim());
					}
					else {
						dm = DICT_PATT2.matcher(line);

						if (dm.find()) {
							String asrPhr = dm.group(1);
							String crtPhr = dm.group(2);

							asrDictionary.put(asrPhr.trim(), crtPhr.trim());
						}
					}
				}
				else if (line.startsWith("CONCEPT ") || line.startsWith("CONCEPT\t")) {
					Matcher cm = CONCEPT_PATT.matcher(line);

					if (cm.find()) {
						String csyn = cm.group(1);
						String ctyp = cm.group(2);
						CType conceptType = null;
						RDConcept scls = null;

						try {
							conceptType = CType.valueOf(ctyp);
						} catch (IllegalArgumentException iae) {
							// Check for an IS-A relation
							for (RDConcept c : definedConcepts) {
								if (c.getCanonicalName().equals(ctyp)) {
									// Found our IS-A relationship
									conceptType = CType.ISA;
									scls = c;
									break;
								}
							}

							if (scls == null) {
								LOGGER.error("'" + ctyp + "' "
										+ "is not a recognized ro.racai.robin.dialog.CType "
										+ "member at line " + lineCount + "!");
								iae.printStackTrace();
								return null;
							}
						}

						if (csyn.contains(",")) {
							List<String> synParts =
									new ArrayList<>(Arrays.asList(csyn.split(COMMA_RX_STR)));
							String canonName = synParts.remove(0);

							definedConcepts.add(RDConcept.conceptBuilder(conceptType, canonName,
									synParts, scls));
						} else {
							definedConcepts.add(RDConcept.conceptBuilder(conceptType, csyn,
									new ArrayList<>(), scls));
						}
					} else {
						LOGGER.warn("CONCEPT line is not well-formed at line " + lineCount + "...");
					}
				} // end CONCEPT keyword
				else if (line.startsWith("REFERENCE ") || line.startsWith("REFERENCE\t")) {
					if (definedConcepts.isEmpty()) {
						LOGGER.error("Found references for missing concepts. "
								+ "Please start with CONCEPT definitions.");
						return null;
					}

					Matcher rm = REFERENCE_PATT.matcher(line);

					if (rm.find()) {
						String canonName = rm.group(1);
						String refString = rm.group(2);
						String refCode = rm.group(3);
						boolean conceptFound = false;

						for (RDConcept c : definedConcepts) {
							if (c.getCanonicalName().equalsIgnoreCase(canonName)) {
								RDConcept nc = c.deepCopy();

								nc.setReference(refString, proc, lex);

								if (!referencedConcepts.containsKey(refCode)) {
									referencedConcepts.put(refCode, nc);
									conceptFound = true;
									break;
								} else {
									LOGGER.error("Reference code '" + refCode
											+ "' is duplicated at line " + lineCount + "!");
									return null;
								}
							}
						}

						if (!conceptFound) {
							LOGGER.error("Found reference for a missing concept '" + canonName
									+ "' at line " + lineCount + ". "
									+ "Please add a CONCEPT definition above.");
							return null;
						}
					} else {
						LOGGER.warn(
								"REFERENCE line is not well-formed at line " + lineCount + "...");
					}
				} // end REFERENCE keyword
				else if (CTYPE_PATT.matcher(line).find()) {
					Matcher ctm = CTYPE_PATT.matcher(line);

					ctm.find();

					CType constType = CType.valueOf(ctm.group(1));
					String constValue = ctm.group(2);
					RDConstant constant = new RDConstant(constType);

					constant.setReference(constValue, proc, lex);

					String constCode = ctm.group(3);

					if (!referencedConcepts.containsKey(constCode)) {
						referencedConcepts.put(constCode, constant);
					} else {
						LOGGER.error("Constant concept code '" + constCode
								+ "' is duplicated at line " + lineCount + "!");
						return null;
					}
				} // end CType keyword
				else if (line.startsWith("PREDICATE ") || line.startsWith("PREDICATE\t")) {
					Matcher pm = PREDICATE_PATT.matcher(line);

					if (pm.find()) {
						String psyn = pm.group(1);
						String usri = pm.group(2);
						UIntentType userIntent = null;

						try {
							userIntent = UIntentType.valueOf(usri);
						} catch (IllegalArgumentException iae) {
							LOGGER.error("'" + usri + "' "
									+ "is not a recognized ro.racai.robin.dialog.UIntentType "
									+ "member at line " + lineCount + "!");
							iae.printStackTrace();
							return null;
						}

						if (psyn.contains(",")) {
							List<String> synParts =
									new ArrayList<>(Arrays.asList(psyn.split(COMMA_RX_STR)));
							String canonName = synParts.remove(0);

							definedPredicates.add(RDPredicate.predicateBuilder(userIntent,
									canonName, synParts));
						} else {
							definedPredicates.add(RDPredicate.predicateBuilder(userIntent, psyn,
									new ArrayList<>()));
						}
					} else {
						LOGGER.warn(
								"PREDICATE line is not well-formed at line " + lineCount + "...");
					}
				} // end PREDICATE keyword
				else if (line.startsWith("TRUE ") || line.startsWith("TRUE\t")) {
					String[] trueParts = line.split("\\s+");
					String actionVerb = trueParts[1];
					boolean predicateFound = false;

					for (RDPredicate p : definedPredicates) {
						if (p.getActionVerb().equalsIgnoreCase(actionVerb)) {
							RDPredicate np = p.deepCopy();

							// Add arguments to the predicate
							for (int i = 2; i < trueParts.length; i++) {
								String refCode = trueParts[i];

								if (referencedConcepts.containsKey(refCode)) {
									np.addArgument(referencedConcepts.get(refCode));
								} else {
									LOGGER.error("Reference code '" + refCode
											+ "' was not declared before at " + lineCount + "!");
									return null;
								}
							} // end all arguments

							predicateFound = true;
							truePredicates.add(np);
							break;
						} // end predicate found
					} // end all defined predicates

					if (!predicateFound) {
						LOGGER.error("Predicate '" + actionVerb
								+ "' was not declared before with a PREDICATE line, at " + lineCount
								+ "!");
						return null;
					}
				} // end TRUE keyword

				line = rdr.readLine();
				lineCount++;
			} // end all .mw file

			RDUniverse universe = new RDUniverse(wn, lex, proc);

			universe.addBoundPredicates(truePredicates);

			for (Map.Entry<String, RDConcept> e : referencedConcepts.entrySet()) {
				universe.addBoundConcept(e.getValue());
			}

			for (RDConcept c : definedConcepts) {
				universe.addConcept(c);
			}

			universe.setASRRulesMap(asrDictionary);

			return universe;
		} catch (IOException e) {
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
