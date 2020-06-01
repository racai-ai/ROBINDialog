/**
 * 
 */
package ro.racai.robin.nlp;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import javax.sound.sampled.LineUnavailableException;
import org.apache.log4j.Logger;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

/**
 * @author Radu Ion ({@code radu@racai.ro})
 * <p>
 * Romanian TTS and ASR.
 * </p>
 */
public class RoSpeechProcessing extends SpeechProcessing {
	private static final Logger LOGGER = Logger.getLogger(RoSpeechProcessing.class.getName());

	private static final String ASR_QUERY = "http://89.38.230.18/upload";
	private static final String TTS_QUERY = "http://89.38.230.18:8080/synthesis";

	public RoSpeechProcessing() {}

	/*
	 * (non-Javadoc)
	 * 
	 * @see ro.racai.robin.nlp.SpeechProcessing#speechToText(java.lang.String)
	 */
	@Override
	public String speechToText() {
		StringBuffer content = new StringBuffer();

		try {
			File wavFile = recordUtterance();
			String LINE_FEED = "\r\n";

			LOGGER.info("Doing ASR on the utterance...");

			try {
				long startTime = System.currentTimeMillis();
				URL url = new URL(RoSpeechProcessing.ASR_QUERY);
				URLConnection conn = url.openConnection();
				HttpURLConnection http = (HttpURLConnection) conn;
				String boundary = "===" + System.currentTimeMillis() + "===";

				http.setRequestMethod("POST");
				http.setDoOutput(true);
				http.setRequestProperty("Content-Type",
						"multipart/form-data; boundary=" + boundary);
				http.connect();

				OutputStream output = http.getOutputStream();
				PrintWriter writer = new PrintWriter(
						new OutputStreamWriter(output, StandardCharsets.US_ASCII), true);

				writer.append("--" + boundary).append(LINE_FEED);
				writer.append("Content-Disposition: form-data; name=\"file\"; filename=\""
						+ wavFile.getName() + "\"").append(LINE_FEED);
				writer.append("Content-Type: audio/wav").append(LINE_FEED);
				writer.append("Content-Transfer-Encoding: binary").append(LINE_FEED);
				writer.append(LINE_FEED);
				writer.flush();

				Files.copy(wavFile.toPath(), output);

				output.flush();
				writer.flush();

				// Finalizing
				writer.append(LINE_FEED).flush();
				writer.append("--" + boundary + "--").append(LINE_FEED);
				writer.close();

				int status = http.getResponseCode();

				if (status == 200) {
					BufferedReader in = new BufferedReader(
							new InputStreamReader(http.getInputStream(), "UTF-8"));
					String line = in.readLine();

					while (line != null) {
						content.append(line);
						line = in.readLine();
					}

					in.close();
				}
				else {
					LOGGER.error("ASR query error; error code " + status);
				}

				long endTime = System.currentTimeMillis();
				long duration = (endTime - startTime);

				LOGGER.info("Elapsed time (s) : " + (duration / 1000));
			}
			catch (UnsupportedEncodingException uee) {
				uee.printStackTrace();
				return null;
			}
			catch (IOException ioe) {
				ioe.printStackTrace();
				return null;
			}
		}
		catch (LineUnavailableException lue) {
			lue.printStackTrace();
		}
		catch (IOException ioe) {
			ioe.printStackTrace();
		}

		String json = content.toString();

		json = json.replace("\\u0218", "Ș");
		json = json.replace("\\u00ce", "Î");
		json = json.replace("\\u00CE", "Î");
		json = json.replace("\\u00c2", "Â");
		json = json.replace("\\u00C2", "Â");
		json = json.replace("\\u0102", "Ă");
		json = json.replace("\\u021a", "Ț");
		json = json.replace("\\u021A", "Ț");

		JSONParser parser = new JSONParser();

		try {
			JSONObject root = (JSONObject) parser.parse(json);
			String transcript = (String) root.get("data");
			Boolean success = (Boolean) root.get("success");

			if (!success) {
				return null;
			} else {
				return transcript.trim().replaceFirst("\\s+\\n$", "").toLowerCase();
			}
		}
		catch (ParseException pe) {
			pe.printStackTrace();
		}

		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see ro.racai.robin.nlp.SpeechProcessing#textToSpeech(java.lang.String)
	 */
	@Override
	public File textToSpeech(String text) {
		String query = new String(RoSpeechProcessing.TTS_QUERY);

		LOGGER.info("Doing TTS on the reply from Pepper...");

		try {
			long startTime = System.currentTimeMillis();
			URL url = new URL(query);
			HttpURLConnection con = (HttpURLConnection) url.openConnection();

			con.setRequestMethod("POST");
			con.setRequestProperty("Content-Type", "application/json");
			
			con.setDoOutput(true);
			con.connect();

			OutputStream output = con.getOutputStream();
			PrintWriter writer =
				new PrintWriter(
					new OutputStreamWriter(output, StandardCharsets.UTF_8), true);

			writer.append("{\"language\": \"ro\", \"speaker\": \"anca\", \"text\": \"" + text + "\"}");
			output.flush();
			writer.flush();
			// Finalizing
			writer.close();

			int status = con.getResponseCode();

			if (status == 200) {
				InputStream is = con.getInputStream();
				byte[] pcm_data = is.readAllBytes();

				is.close();

				FileOutputStream fos = new FileOutputStream("out.wav");

				fos.write(pcm_data);
				fos.flush();
				fos.close();

				long endTime = System.currentTimeMillis();
				long duration = (endTime - startTime);

				LOGGER.info("Elapsed time (s) : " + (duration / 1000));

				return new File("out.wav");
			}
			else {
				LOGGER.error("TTS query error; error code " + status);
			}
		}
		catch (IOException ioe) {
			ioe.printStackTrace();
		}

		return null;
	}
}
