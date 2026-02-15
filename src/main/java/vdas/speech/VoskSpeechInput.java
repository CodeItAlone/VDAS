package vdas.speech;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.vosk.LogLevel;
import org.vosk.LibVosk;
import org.vosk.Model;
import org.vosk.Recognizer;

import javax.sound.sampled.*;
import java.io.File;

/**
 * Offline speech-to-text implementation using Vosk.
 * Captures audio from the default microphone, runs recognition locally,
 * and returns the recognized text as a plain string.
 */
public class VoskSpeechInput implements SpeechInput {

    private static final String DEFAULT_MODEL_PATH = "C:/vosk-models/vosk-model-small-en-us-0.15";
    private static final float SAMPLE_RATE = 16000;
    private static final int SAMPLE_SIZE_BITS = 16;
    private static final int CHANNELS = 1;
    private static final boolean SIGNED = true;
    private static final boolean BIG_ENDIAN = false;
    private static final int BUFFER_SIZE = 4096;
    private static final int MAX_LISTEN_SECONDS = 10;
    private static final int SILENCE_THRESHOLD = 500; // amplitude threshold
    private static final int SILENCE_TIMEOUT_MS = 3000; // stop after 3s silence

    private final Model model;

    /**
     * Creates a VoskSpeechInput using the default model path ("model" in working
     * directory).
     *
     * @throws RuntimeException if the model directory is missing or invalid
     */
    public VoskSpeechInput() {
        this(DEFAULT_MODEL_PATH);
    }

    /**
     * Creates a VoskSpeechInput with a custom model path.
     *
     * @param modelPath path to the Vosk model directory
     * @throws RuntimeException if the model directory is missing or invalid
     */
    public VoskSpeechInput(String modelPath) {
        File modelDir = new File(modelPath);
        if (!modelDir.exists() || !modelDir.isDirectory()) {
            throw new RuntimeException(
                    "Vosk model not found at '" + modelDir.getAbsolutePath() + "'. " +
                            "Download from https://alphacephei.com/vosk/models and extract to '" + modelPath + "/'.");
        }
        LibVosk.setLogLevel(LogLevel.WARNINGS);
        try {
            this.model = new Model(modelPath);
        } catch (java.io.IOException e) {
            throw new RuntimeException("Failed to load Vosk model: " + e.getMessage(), e);
        }
        System.out.println("[VOICE] Vosk model loaded successfully.");
    }

    @Override
    public String listen() {
        AudioFormat format = new AudioFormat(SAMPLE_RATE, SAMPLE_SIZE_BITS, CHANNELS, SIGNED, BIG_ENDIAN);
        DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);

        if (!AudioSystem.isLineSupported(info)) {
            System.err.println("[VOICE] Microphone not available or audio format not supported.");
            return "";
        }

        try (Recognizer recognizer = new Recognizer(model, SAMPLE_RATE)) {
            TargetDataLine microphone = (TargetDataLine) AudioSystem.getLine(info);
            microphone.open(format);
            microphone.start();

            System.out.println("[VOICE] Listening... (speak now, max " + MAX_LISTEN_SECONDS + "s)");

            byte[] buffer = new byte[BUFFER_SIZE];
            long startTime = System.currentTimeMillis();
            long lastSoundTime = System.currentTimeMillis();
            boolean heardSpeech = false;

            while (true) {
                long elapsed = System.currentTimeMillis() - startTime;
                if (elapsed > MAX_LISTEN_SECONDS * 1000L) {
                    System.out.println("[VOICE] Max listen time reached.");
                    break;
                }

                // Stop if silence persists after hearing speech
                if (heardSpeech && (System.currentTimeMillis() - lastSoundTime > SILENCE_TIMEOUT_MS)) {
                    System.out.println("[VOICE] Silence detected, finalizing...");
                    break;
                }

                int bytesRead = microphone.read(buffer, 0, buffer.length);
                if (bytesRead <= 0)
                    continue;

                // Check if audio contains speech (simple amplitude check)
                if (containsSound(buffer, bytesRead)) {
                    lastSoundTime = System.currentTimeMillis();
                    heardSpeech = true;
                }

                if (recognizer.acceptWaveForm(buffer, bytesRead)) {
                    String result = extractText(recognizer.getResult());
                    if (!result.isEmpty()) {
                        microphone.stop();
                        microphone.close();
                        return result;
                    }
                }
            }

            microphone.stop();
            microphone.close();

            // Get final result
            String finalResult = extractText(recognizer.getFinalResult());
            if (!finalResult.isEmpty()) {
                return finalResult;
            }

            System.out.println("[VOICE] No speech recognized.");
            return "";

        } catch (LineUnavailableException e) {
            System.err.println("[VOICE] Microphone error: " + e.getMessage());
            return "";
        } catch (Exception e) {
            System.err.println("[VOICE] Recognition error: " + e.getMessage());
            return "";
        }
    }

    @Override
    public void close() {
        if (model != null) {
            model.close();
            System.out.println("[VOICE] Vosk model released.");
        }
    }

    /**
     * Extracts the "text" field from Vosk JSON output.
     * Vosk returns results like: {"text" : "java version"}
     */
    private String extractText(String jsonResult) {
        try {
            JsonObject json = JsonParser.parseString(jsonResult).getAsJsonObject();
            String text = json.has("text") ? json.get("text").getAsString().trim() : "";
            return text;
        } catch (Exception e) {
            System.err.println("[VOICE] Failed to parse recognizer output: " + e.getMessage());
            return "";
        }
    }

    /**
     * Simple check if the audio buffer contains sound above the silence threshold.
     */
    private boolean containsSound(byte[] buffer, int bytesRead) {
        for (int i = 0; i < bytesRead - 1; i += 2) {
            short sample = (short) ((buffer[i + 1] << 8) | (buffer[i] & 0xFF));
            if (Math.abs(sample) > SILENCE_THRESHOLD) {
                return true;
            }
        }
        return false;
    }
}
