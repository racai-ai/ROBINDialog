package ro.racai.robin.nlp;

import java.io.BufferedWriter;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import com.ineo.nlp.language.LanguagePipe;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.Port;
import javax.sound.sampled.UnsupportedAudioFileException;
import org.apache.log4j.Logger;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import ssla.SSLA;

public class RoSpeechProcessing2 extends SpeechProcessing {
	private static final Logger LOGGER = Logger.getLogger(RoSpeechProcessing2.class.getName());
	private static final String SSLA_MODEL = "speech\\ssla\\models\\ro\\anca";
	private static final String MLPLA_MODEL = "speech\\mlpla\\models\\ro";
	private static final String MLPLA_CONF = "speech\\mlpla\\etc\\languagepipe.conf";
	private static final String MLPLA_OUTFILE = "input.lab";
	private static final String ASR_QUERY = "http://relate.racai.ro:7001/transcribe";

	@Override
	public String speechToText() {
		StringBuilder content = new StringBuilder();

		try {
			File wavFile = recordUtterance();
			String lineFeed = "\r\n";

			LOGGER.info("Doing ASR on the utterance...");

			try {
				long startTime = System.currentTimeMillis();
				URL url = new URL(RoSpeechProcessing2.ASR_QUERY);
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
					try (BufferedReader in = new BufferedReader(
							new InputStreamReader(http.getInputStream(), StandardCharsets.UTF_8))) {
						String line = in.readLine();

						while (line != null) {
							content.append(line);
							line = in.readLine();
						}
					}
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
			String transcription = (String) root.get("transcription");
			String status = (String) root.get("status");

			if (!status.equals("OK")) {
				return null;
			} else {
				return transcription.trim().replaceFirst("\\s+\\n$", "").toLowerCase();
			}
		} catch (ParseException pe) {
			pe.printStackTrace();
		}

		return null;
	}

	/**
	 * Using SSLA by Tiberiu Boroș. See here: https://arxiv.org/pdf/1802.05583.pdf
	 */
	@Override
	public File textToSpeech(String text) {
		// 0. Always append the final period for SSLA to work properly!
		if (!text.endsWith(".")) {
			text += ".";
		}

		// 1. Write the text to the lab.txt file
		try (BufferedWriter wrt =
				new BufferedWriter(new OutputStreamWriter(new FileOutputStream("input.txt"),
						StandardCharsets.UTF_8))) {
			wrt.write(text);
			wrt.newLine();
		} catch (IOException ioe) {
			ioe.printStackTrace();
			return null;
		}

		// 2. Preprocess the lab.txt file
		try (PrintStream redirect =
				new PrintStream(new FileOutputStream(new File(MLPLA_OUTFILE)), true, "UTF-8")) {

			PrintStream original = System.out;
			System.setOut(redirect);
			String[] args = {"--process", MLPLA_CONF, MLPLA_MODEL, "input.txt"};
			LanguagePipe.main(args);
			System.setOut(original);
		} catch (ClassNotFoundException cnfe) {
			cnfe.printStackTrace();
			return null;
		} catch (InstantiationException ie) {
			ie.printStackTrace();
			return null;
		} catch (IllegalAccessException iae) {
			iae.printStackTrace();
			return null;
		} catch (IOException ioe) {
			ioe.printStackTrace();
			return null;
		}

		try {
			String[] args = {"--synthesize", SSLA_MODEL, MLPLA_OUTFILE, "out.wav", "true"};
			SSLA.main(args);

			return new File("out.wav");
		} catch (IOException ioe) {
			ioe.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
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
			// PCM_SIGNED 48000.0 Hz, 16 bit, mono, 2 bytes/frame, little-endian
			if (format.getFrameSize() != 2 || format.getSampleSizeInBits() != 16
					|| format.getChannels() != 1 || format.isBigEndian()) {
				throw new UnsupportedAudioFileException();
			}

			byte[] pcmData = original.readAllBytes();
			AudioFormat pformat = new AudioFormat(format.getSampleRate(), 16, 1, true, false);

			if (speakersClipLine == null) {
				// Only do this once, for the lifetime
				// of this object.
				speakersClipLine = findSpeakers();
			}

			speakersClipLine.open(pformat, pcmData, 0, pcmData.length);
			speakersClipLine.start();
			speakersClipLine.drain();

			// Wait for the clip to play...
			float waitTimeSecs = (pcmData.length / 2.0f) / format.getSampleRate();

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
