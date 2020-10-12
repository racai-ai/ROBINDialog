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
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Port;
import javax.sound.sampled.UnsupportedAudioFileException;
import org.apache.log4j.Logger;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;


/**
 * @author Radu Ion ({@code radu@racai.ro})
 *         <p>
 *         Romanian TTS and ASR.
 *         </p>
 */
public class RoSpeechProcessing extends SpeechProcessing {
	private static final Logger LOGGER = Logger.getLogger(RoSpeechProcessing.class.getName());

	private static final String ASR_QUERY = "http://89.38.230.18/upload";
	private static final String TTS_QUERY = "http://89.38.230.18:8080/synthesis";

	/*
	 * (non-Javadoc)
	 * 
	 * @see ro.racai.robin.nlp.SpeechProcessing#speechToText(java.lang.String)
	 */
	@Override
	public String speechToText() {
		StringBuilder content = new StringBuilder();

		try {
			File wavFile = recordUtterance();
			String lineFeed = "\r\n";

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

				writer.append("--" + boundary).append(lineFeed);
				writer.append("Content-Disposition: form-data; name=\"file\"; filename=\""
						+ wavFile.getName() + "\"").append(lineFeed);
				writer.append("Content-Type: audio/wav").append(lineFeed);
				writer.append("Content-Transfer-Encoding: binary").append(lineFeed);
				writer.append(lineFeed);
				writer.flush();

				Files.copy(wavFile.toPath(), output);

				output.flush();
				writer.flush();

				// Finalizing
				writer.append(lineFeed).flush();
				writer.append("--" + boundary + "--").append(lineFeed);
				writer.close();

				int status = http.getResponseCode();

				if (status == 200) {
					BufferedReader in = new BufferedReader(
							new InputStreamReader(http.getInputStream(), StandardCharsets.UTF_8));
					String line = in.readLine();

					while (line != null) {
						content.append(line);
						line = in.readLine();
					}

					in.close();
				} else {
					LOGGER.error("ASR query error; error code " + status);
				}

				long endTime = System.currentTimeMillis();
				long duration = (endTime - startTime);

				LOGGER.info("Elapsed time (s) : " + (duration / 1000));
			} catch (UnsupportedEncodingException uee) {
				uee.printStackTrace();
				return null;
			} catch (IOException ioe) {
				ioe.printStackTrace();
				return null;
			}
		} catch (LineUnavailableException lue) {
			lue.printStackTrace();
		} catch (IOException ioe) {
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
			boolean success = (Boolean) root.get("success");

			if (!success) {
				return null;
			} else {
				return transcript.trim().replaceFirst("\\s+\\n$", "").toLowerCase();
			}
		} catch (ParseException pe) {
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
		LOGGER.info("Doing TTS on the reply from Pepper...");

		try {
			long startTime = System.currentTimeMillis();
			URL url = new URL(RoSpeechProcessing.TTS_QUERY);
			HttpURLConnection con = (HttpURLConnection) url.openConnection();

			con.setRequestMethod("POST");
			con.setRequestProperty("Content-Type", "application/json");

			con.setDoOutput(true);
			con.connect();

			OutputStream output = con.getOutputStream();
			PrintWriter writer =
					new PrintWriter(new OutputStreamWriter(output, StandardCharsets.UTF_8), true);

			writer.append(
					"{\"language\": \"ro\", \"speaker\": \"anca\", \"text\": \"" + text + "\"}");
			output.flush();
			writer.flush();
			// Finalizing
			writer.close();

			int status = con.getResponseCode();

			if (status == 200) {
				InputStream is = con.getInputStream();
				byte[] pcmData = is.readAllBytes();

				is.close();

				try (FileOutputStream fos = new FileOutputStream("out.wav")) {
					fos.write(pcmData);
					fos.flush();
				} catch (IOException ioe) {
					ioe.printStackTrace();
				}

				long endTime = System.currentTimeMillis();
				long duration = (endTime - startTime);

				LOGGER.info("Elapsed time (s) : " + (duration / 1000));

				return new File("out.wav");
			} else {
				LOGGER.error("TTS query error; error code " + status);
			}
		} catch (IOException ioe) {
			ioe.printStackTrace();
		}

		return null;
	}

	@Override
	public void playUtterance(File wavFile) throws LineUnavailableException,
			UnsupportedAudioFileException, InterruptedException, IOException {
		if (AudioSystem.isLineSupported(Port.Info.SPEAKER)) {
			AudioInputStream original = AudioSystem.getAudioInputStream(wavFile);
			AudioFormat format = original.getFormat();

			// Match this with the incoming TTS WAV format!
			// PCM_FLOAT 24000.0 Hz, 32 bit, mono, 4 bytes/frame
			if (format.getFrameSize() != 4 || format.getSampleSizeInBits() != 32
					|| format.getChannels() != 1 || format.isBigEndian()) {
				throw new UnsupportedAudioFileException();
			}

			byte[] pcmData = original.readAllBytes();
			// Going from 32 bits to 16 bits
			byte[] pcmDataConverted = new byte[pcmData.length / 2];
			int j = 0;

			for (int i = 0; i < pcmData.length; i += 4) {
				byte b0 = pcmData[i];
				byte b1 = pcmData[i + 1];
				byte b2 = pcmData[i + 2];
				byte b3 = pcmData[i + 3];
				byte[] pcmFloat = new byte[] {b0, b1, b2, b3};

				// https://www.scadacore.com/tools/programming-calculators/online-hex-converter/
				// String bytesPrint = "";
				// bytesPrint += String.format("%2x", b0).replace(" ", "0");
				// bytesPrint += String.format("%2x", b1).replace(" ", "0");
				// bytesPrint += String.format("%2x", b2).replace(" ", "0");
				// bytesPrint += String.format("%2x", b3).replace(" ", "0");
				// System.out.println(bytesPrint);

				// Data comes in LITTLE_ENDIAN
				ByteBuffer floatBuffer = ByteBuffer.wrap(pcmFloat).order(ByteOrder.LITTLE_ENDIAN);
				// PCM_FLOAT range is -1.0 to 1.0
				float floatDataPoint = floatBuffer.getFloat();
				// So it has to be scaled with Short.MAX_VALUE
				// for 16 bit conversion
				// https://www.kvraudio.com/forum/viewtopic.php?t=414666
				// http://blog.bjornroche.com/2009/12/int-float-int-its-jungle-out-there.html
				float scaledDataPoint = Short.MAX_VALUE * floatDataPoint;
				short shortDataPoint;

				if (scaledDataPoint > Short.MAX_VALUE) {
					shortDataPoint = Short.MAX_VALUE;
				} else if (scaledDataPoint < Short.MIN_VALUE) {
					shortDataPoint = Short.MIN_VALUE;
				} else {
					shortDataPoint = (short) scaledDataPoint;
				}

				ByteBuffer shortBuffer = ByteBuffer.allocate(2).order(ByteOrder.LITTLE_ENDIAN);

				shortBuffer.putShort(shortDataPoint);

				byte[] shortBufferArray = shortBuffer.array();

				for (int k = 0; k < shortBufferArray.length; k++) {
					pcmDataConverted[j] = shortBufferArray[k];
					j++;
				}
			}

			AudioFormat pformat = new AudioFormat(format.getSampleRate(), 16, 1, true, false);

			if (speakersClipLine == null) {
				// Only do this once, for the lifetime
				// of this object.
				speakersClipLine = findSpeakers();
			}

			speakersClipLine.open(pformat, pcmDataConverted, 0, pcmDataConverted.length);
			speakersClipLine.start();
			speakersClipLine.drain();

			// Wait for the clip to play...
			float waitTimeSecs = (pcmDataConverted.length / 2.0f) / format.getSampleRate();

			try {
				Thread.sleep((long) (1000.0f * waitTimeSecs));
			} catch (InterruptedException e) {
				e.printStackTrace();
				throw e;
			}
			speakersClipLine.stop();
			speakersClipLine.close();

			return;
		}

		throw new LineUnavailableException();
	}
}
