/**
 * 
 */
package ro.racai.robin.nlp;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.Line;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.UnsupportedAudioFileException;
import javax.sound.sampled.TargetDataLine;
import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.Port;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.log4j.Logger;

/**
 * @author Radu Ion ({@code radu@racai.ro}
 * 
 * <p>
 * Handles the ASR and TTS routines for ROBIN Dialogue.
 * </p>
 */
public abstract class SpeechProcessing {
	private static final Logger LOGGER = Logger.getLogger(SpeechProcessing.class.getName());

	private LinkedList<Float> silenceSamplesStdDevs = new LinkedList<>();

	/**
	 * How many silence averages to maintain to get an accurate estimation on what it means to be
	 * silent, in terms of sample values.
	 */
	private static final int SILENCE_SAMPLES_STDDEVS_LENGTH = 100;

	/**
	 * After how many consecutive silence samples do we stop recording the voice.
	 */
	private static final int CONSECUTIVE_SILENCE_SAMPLES = 20;

	/**
	 * After how many consecutive speech samples do we start recording speech.
	 */
	private static final int CONSECUTIVE_SPEECH_SAMPLES = 5;

	/**
	 * Value computed from {@link #silenceSamplesStdDevsLength}.
	 */
	private float silenceSignalLevel = 0.0f;

	/**
	 * If using a webcam microphone, leave it at {@code 1.5}, if using a dedicated microphone, you
	 * can raise it to e.g. {@code 10.0}.
	 */
	private static final float SIGNAL_TO_SILENCE_RATIO = 10.0f;

	protected Clip speakersClipLine = null;

	/**
	 * <p>
	 * Records a .wav file stored on the local hard disk and converts it to the said text.
	 * </p>
	 * 
	 * @return the said text.
	 */
	public abstract String speechToText();

	/**
	 * <p>
	 * Takes a Java {@link String} representing a saying in the given language and produces the
	 * corresponding utterance from it.
	 * </p>
	 * 
	 * @param text the text to be said;
	 * @return the .wav file stored on the local HDD.
	 */
	public abstract File textToSpeech(String text);

	// Assumes we have 16 bit samples, 2 bytes/sample, ByteOrder.LITTLE_ENDIAN
	private List<Float> pcmDataPoints(byte[] vector) {
		List<Float> dataPoints = new ArrayList<>();

		for (int i = 0; i < vector.length; i += 2) {
			// LSB, MSB
			byte[] twobytes = new byte[] {vector[i], vector[i + 1]};
			ByteBuffer shortBuff =
				ByteBuffer.wrap(twobytes).order(ByteOrder.LITTLE_ENDIAN);
			short dataPoint = shortBuff.getShort();

			dataPoints.add((float) dataPoint);
		}

		return dataPoints;
	}

	private float signalAverage(List<Float> vector) {
		float sum = 0.0f;

		for (Float f : vector) {
			sum += f;
		}

		return sum / (float) vector.size();
	}

	private float signalStandardDeviation(byte[] vector) {
		List<Float> floatVector = pcmDataPoints(vector);
		float m = signalAverage(floatVector);
		float sum = 0.0f;

		for (Float f : floatVector) {
			sum += (f - m) * (f - m);
		}

		return (float) Math.sqrt(sum / (float) floatVector.size());
	}

	private boolean isSilence(float sampleStdDev) {
		// Collect some silence data first,
		// before making silence/non-silence judgments.
		if (silenceSamplesStdDevs.isEmpty()) {
			return true;
		}

		float sum = 0.0f;

		for (float a : silenceSamplesStdDevs) {
			sum += a;
		}

		silenceSignalLevel = sum / (float) silenceSamplesStdDevs.size();

		return !(silenceSignalLevel > 0.0f
				&& (sampleStdDev / silenceSignalLevel) >= SIGNAL_TO_SILENCE_RATIO);
	}

	protected Clip findSpeakers() throws LineUnavailableException {
		Mixer.Info[] mixerInfo = AudioSystem.getMixerInfo();

		for (Mixer.Info mnf : mixerInfo) {
			if (!mnf.getName().matches("^.*[sS][pP][eE][aA][kK][eE][rR].*$")
					&& !mnf.getName().matches("^.*[hH][eE][aA][dD][pP][hH][oO][nN][eE].*$")) {
				continue;
			}

			Mixer mixer = AudioSystem.getMixer(mnf);
			Line.Info[] lineInfo = mixer.getSourceLineInfo();

			for (Line.Info lnf : lineInfo) {
				Line line = mixer.getLine(lnf);

				try {
					return (Clip) line;
				} catch (ClassCastException e) {
					// No need to do anything.
				}
			}
		}

		throw new LineUnavailableException();
	}

	/**
	 * <p>
	 * Records a message from the default microphone of the system.
	 * </p>
	 * 
	 * @return the .wav file with the recorded utterance.
	 * @throws LineUnavailableException if there is no microphone available.
	 * @throws IOException              if .wav file cannot be written.
	 */
	protected File recordUtterance() throws LineUnavailableException, IOException {
		if (AudioSystem.isLineSupported(Port.Info.MICROPHONE)) {
			AudioFormat format = new AudioFormat(44100.0f, 16, 1, true, false);
			TargetDataLine microphone = AudioSystem.getTargetDataLine(format);

			// 0. Open and start the microphone
			microphone.open();
			microphone.start();

			// 0.1 Will hold the last 3 data buffers
			// to save them all into the .wav file
			// when speech is detected.
			List<byte[]> dataBuffers = new ArrayList<>();
			int dbi = 0;
			final int dbSize = CONSECUTIVE_SPEECH_SAMPLES + 5 * CONSECUTIVE_SILENCE_SAMPLES;

			for (int i = 0; i < dbSize; i++) {
				dataBuffers.add(new byte[microphone.getBufferSize() / 5]);
			}

			byte[] data = dataBuffers.get(dbi);

			microphone.read(data, 0, data.length);

			float dataStdDev = signalStandardDeviation(data);
			boolean silenceFlag = isSilence(dataStdDev);

			LOGGER.info("Listening...");

			int speechCount = 0;

			// 1. Remain in this loop while
			// there is no speech detected.
			while (silenceFlag || speechCount < CONSECUTIVE_SPEECH_SAMPLES) {
				dbi++;

				if (dbi % dbSize == 0) {
					dbi = 0;
				}

				data = dataBuffers.get(dbi);

				// Read the next chunk of data from the TargetDataLine.
				microphone.read(data, 0, data.length);
				dataStdDev = signalStandardDeviation(data);

				silenceFlag = isSilence(dataStdDev);

				if (silenceFlag) {
					speechCount = 0;

					if (silenceSamplesStdDevs.size() == SILENCE_SAMPLES_STDDEVS_LENGTH) {
						silenceSamplesStdDevs.removeFirst();
					}

					if (dbi % 10 == 0) {
						silenceSamplesStdDevs.add(dataStdDev);
						// LOGGER.info("Instant silence std. dev. = " + dataStdDev);
					}
				} else {
					// LOGGER.info("Instant speech #" + speechCount + " std. dev. = " + dataStdDev);
					speechCount++;
				}
			} // end silence loop

			LOGGER.info("Speech detected! Speech std. dev. = " + dataStdDev
					+ ", Silence std. dev. = " + silenceSignalLevel);

			// 2. Collect speech samples and save them into voiceData
			LinkedList<Byte> voiceData = new LinkedList<>();
			int silenceCount = 0;

			// 2.2 Here we record the actual speech
			// coming from the microphone
			do {
				for (byte b : data) {
					voiceData.add(b);
				}

				// Read the next chunk of data from the TargetDataLine.
				microphone.read(data, 0, data.length);
				dataStdDev = signalStandardDeviation(data);

				// LOGGER.info("Speech std. dev. = " + dataStdDev);

				silenceFlag = isSilence(dataStdDev);

				if (silenceFlag) {
					silenceCount++;
				} else {
					silenceCount = 0;
				}
			} // end speech loop
			while (!silenceFlag || silenceCount < CONSECUTIVE_SILENCE_SAMPLES);

			LOGGER.info("Speech stopped.");

			// 2.3 Collect the last buffers as well
			// to get the start of the syllable.
			int dbc = 0;

			dbi--;

			while (dbi >= 0
					&& dbc < CONSECUTIVE_SPEECH_SAMPLES + (CONSECUTIVE_SILENCE_SAMPLES / 2)) {
				byte[] pastdata = dataBuffers.get(dbi);

				for (int i = pastdata.length - 1; i >= 0; i--) {
					voiceData.addFirst(pastdata[i]);
				}

				dbi--;
				dbc++;
			}

			// 3. And save it to the .wav file
			byte[] pcmData = ArrayUtils.toPrimitive(voiceData.toArray(new Byte[0]));

			AudioInputStream ais = new AudioInputStream(new ByteArrayInputStream(pcmData), format,
					pcmData.length / format.getFrameSize());
			File wav = new File("test.wav");
			AudioSystem.write(ais, AudioFileFormat.Type.WAVE, wav);

			// 4. Release the microphone resource
			microphone.stop();
			microphone.close();

			return wav;
		} else {
			throw new LineUnavailableException();
		}
	}	

	/**
	 * <p>
	 * Plays a generated .wav file, saying whatever the dialog system has produced in text.
	 * </p>
	 * 
	 * @param wavFile the .wav file to play with available speakers.
	 * @throws InterruptedException
	 */
	public abstract void playUtterance(File wavFile)
			throws LineUnavailableException, UnsupportedAudioFileException, InterruptedException, IOException;
}
