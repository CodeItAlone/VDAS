package vdas.speech;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import javax.sound.sampled.*;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Whisper-based speech-to-text implementation.
 * <p>
 * Records audio from the default microphone (16kHz, mono, 16-bit PCM signed,
 * little-endian), saves it as {@code command.wav}, and sends it to a local
 * Python Whisper STT service via HTTP multipart/form-data.
 * <p>
 * Implements RMS-based silence detection with configurable thresholds.
 * Uses only standard Java APIs — no JNI, no native bindings, no ML.
 */
public class WhisperSpeechInput implements SpeechInput {

    // ── Audio format ────────────────────────────────────────────
    private static final float SAMPLE_RATE = 16000f;
    private static final int SAMPLE_SIZE_BITS = 16;
    private static final int CHANNELS = 1;
    private static final boolean SIGNED = true;
    private static final boolean BIG_ENDIAN = false;

    // ── Recording limits ────────────────────────────────────────
    private static final int BUFFER_SIZE = 4096;
    private static final int MAX_RECORD_SECONDS = 5;
    private static final int SILENCE_TIMEOUT_MS = 1000; // stop after 1s silence
    private static final double SILENCE_RMS_THRESHOLD = 300.0;

    // ── HTTP ────────────────────────────────────────────────────
    private static final int HTTP_CONNECT_TIMEOUT_MS = 3000;
    private static final int HTTP_READ_TIMEOUT_MS = 15000; // Whisper inference can take a few seconds

    private final String serverUrl;
    private final String wavFilePath;

    /**
     * Creates a WhisperSpeechInput with default server URL and WAV path.
     */
    public WhisperSpeechInput() {
        this("http://localhost:8000", "command.wav");
    }

    /**
     * Creates a WhisperSpeechInput with a custom server URL and WAV path.
     *
     * @param serverUrl   base URL of the Whisper STT service (e.g.
     *                    "http://localhost:8000")
     * @param wavFilePath path to save the recorded WAV file
     */
    public WhisperSpeechInput(String serverUrl, String wavFilePath) {
        this.serverUrl = serverUrl;
        this.wavFilePath = wavFilePath;
    }

    /**
     * Checks if the Whisper STT service is reachable.
     *
     * @return true if /health returns HTTP 200
     */
    public boolean isServiceAvailable() {
        try {
            URL url = new URL(serverUrl + "/health");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(HTTP_CONNECT_TIMEOUT_MS);
            conn.setReadTimeout(HTTP_CONNECT_TIMEOUT_MS);
            int status = conn.getResponseCode();
            conn.disconnect();
            return status == 200;
        } catch (IOException e) {
            return false;
        }
    }

    @Override
    public String listen() {
        // Step 1: Record audio to WAV
        boolean recorded = recordToWav();
        if (!recorded) {
            return "";
        }

        // Step 2: Send WAV to Whisper service and get text
        return sendAndParse();
    }

    @Override
    public void close() {
        // Clean up WAV file if it exists
        File wavFile = new File(wavFilePath);
        if (wavFile.exists()) {
            wavFile.delete();
        }
    }

    // ─────────────────────────────────────────────────────────────
    // AUDIO RECORDING
    // ─────────────────────────────────────────────────────────────

    /**
     * Records microphone audio to a WAV file with RMS-based silence detection.
     *
     * @return true if audio was recorded successfully, false on error or pure
     *         silence
     */
    private boolean recordToWav() {
        AudioFormat format = new AudioFormat(SAMPLE_RATE, SAMPLE_SIZE_BITS, CHANNELS, SIGNED, BIG_ENDIAN);
        DataLine.Info lineInfo = new DataLine.Info(TargetDataLine.class, format);

        if (!AudioSystem.isLineSupported(lineInfo)) {
            System.err.println("[WHISPER] Microphone not available or audio format not supported.");
            return false;
        }

        try {
            TargetDataLine microphone = (TargetDataLine) AudioSystem.getLine(lineInfo);
            microphone.open(format);
            microphone.start();

            System.out.println("[WHISPER] Listening... (speak now, max " + MAX_RECORD_SECONDS + "s)");

            ByteArrayOutputStream audioData = new ByteArrayOutputStream();
            byte[] buffer = new byte[BUFFER_SIZE];

            long startTime = System.currentTimeMillis();
            long lastSoundTime = System.currentTimeMillis();
            boolean heardSpeech = false;

            while (true) {
                long elapsed = System.currentTimeMillis() - startTime;

                // Hard max duration
                if (elapsed > MAX_RECORD_SECONDS * 1000L) {
                    System.out.println("[WHISPER] Max record time reached.");
                    break;
                }

                // Silence timeout: stop if silence persists after hearing speech
                if (heardSpeech && (System.currentTimeMillis() - lastSoundTime > SILENCE_TIMEOUT_MS)) {
                    System.out.println("[WHISPER] Silence detected, finalizing...");
                    break;
                }

                int bytesRead = microphone.read(buffer, 0, buffer.length);
                if (bytesRead <= 0) {
                    continue;
                }

                audioData.write(buffer, 0, bytesRead);

                // RMS-based silence detection
                double rms = calculateRms(buffer, bytesRead);
                if (rms > SILENCE_RMS_THRESHOLD) {
                    lastSoundTime = System.currentTimeMillis();
                    heardSpeech = true;
                }
            }

            microphone.stop();
            microphone.close();

            if (!heardSpeech) {
                System.out.println("[WHISPER] No speech detected.");
                return false;
            }

            // Write WAV file
            byte[] rawAudio = audioData.toByteArray();
            writeWavFile(rawAudio, format);

            System.out.println("[WHISPER] Audio saved: " + wavFilePath
                    + " (" + (rawAudio.length / (int) (SAMPLE_RATE * 2)) + "s)");
            return true;

        } catch (LineUnavailableException e) {
            System.err.println("[WHISPER] Microphone error: " + e.getMessage());
            return false;
        } catch (IOException e) {
            System.err.println("[WHISPER] Failed to save WAV: " + e.getMessage());
            return false;
        }
    }

    /**
     * Calculates the Root Mean Square (RMS) of 16-bit PCM audio samples.
     */
    private double calculateRms(byte[] buffer, int bytesRead) {
        long sumSquares = 0;
        int sampleCount = bytesRead / 2; // 16-bit = 2 bytes per sample

        for (int i = 0; i < bytesRead - 1; i += 2) {
            // Little-endian 16-bit signed sample
            short sample = (short) ((buffer[i + 1] << 8) | (buffer[i] & 0xFF));
            sumSquares += (long) sample * sample;
        }

        return Math.sqrt((double) sumSquares / sampleCount);
    }

    /**
     * Writes raw PCM data as a WAV file (16kHz, mono, 16-bit signed,
     * little-endian).
     */
    private void writeWavFile(byte[] rawAudio, AudioFormat format) throws IOException {
        ByteArrayInputStream bais = new ByteArrayInputStream(rawAudio);
        long frameCount = rawAudio.length / format.getFrameSize();
        AudioInputStream ais = new AudioInputStream(bais, format, frameCount);

        File wavFile = new File(wavFilePath);
        AudioSystem.write(ais, AudioFileFormat.Type.WAVE, wavFile);
        ais.close();
    }

    // ─────────────────────────────────────────────────────────────
    // HTTP CLIENT
    // ─────────────────────────────────────────────────────────────

    /**
     * Sends command.wav to the Whisper STT service and parses the JSON response.
     *
     * @return transcribed text (lowercase, trimmed), or empty string on failure
     */
    private String sendAndParse() {
        File wavFile = new File(wavFilePath);
        if (!wavFile.exists() || wavFile.length() == 0) {
            System.err.println("[WHISPER] No audio file to send.");
            return "";
        }

        String boundary = "----VDASBoundary" + System.currentTimeMillis();

        try {
            URL url = new URL(serverUrl + "/transcribe");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setConnectTimeout(HTTP_CONNECT_TIMEOUT_MS);
            conn.setReadTimeout(HTTP_READ_TIMEOUT_MS);
            conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);

            // Write multipart body
            try (OutputStream os = conn.getOutputStream()) {
                writeMultipartFile(os, boundary, "file", wavFile, "audio/wav");
                // Final boundary
                os.write(("--" + boundary + "--\r\n").getBytes());
                os.flush();
            }

            // Read response
            int responseCode = conn.getResponseCode();
            if (responseCode != 200) {
                String error = readStream(conn.getErrorStream());
                System.err.println("[WHISPER] Server error (HTTP " + responseCode + "): " + error);
                conn.disconnect();
                return "";
            }

            String responseBody = readStream(conn.getInputStream());
            conn.disconnect();

            // Parse JSON: {"text": "..."}
            return extractText(responseBody);

        } catch (java.net.ConnectException e) {
            System.err.println("[WHISPER] Cannot connect to STT service at " + serverUrl
                    + ". Is the Python server running?");
            return "";
        } catch (java.net.SocketTimeoutException e) {
            System.err.println("[WHISPER] STT service timed out: " + e.getMessage());
            return "";
        } catch (IOException e) {
            System.err.println("[WHISPER] HTTP error: " + e.getMessage());
            return "";
        }
    }

    /**
     * Writes a single file part in multipart/form-data format.
     */
    private void writeMultipartFile(OutputStream os, String boundary,
            String fieldName, File file, String contentType) throws IOException {
        StringBuilder header = new StringBuilder();
        header.append("--").append(boundary).append("\r\n");
        header.append("Content-Disposition: form-data; name=\"").append(fieldName)
                .append("\"; filename=\"").append(file.getName()).append("\"\r\n");
        header.append("Content-Type: ").append(contentType).append("\r\n");
        header.append("\r\n");
        os.write(header.toString().getBytes());

        // Stream file bytes
        try (FileInputStream fis = new FileInputStream(file)) {
            byte[] buf = new byte[4096];
            int len;
            while ((len = fis.read(buf)) != -1) {
                os.write(buf, 0, len);
            }
        }

        os.write("\r\n".getBytes());
    }

    /**
     * Reads an input stream to a string. Returns empty string if stream is null.
     */
    private String readStream(InputStream stream) throws IOException {
        if (stream == null) {
            return "";
        }
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            return sb.toString();
        }
    }

    /**
     * Extracts the "text" field from a JSON response string.
     */
    private String extractText(String jsonResponse) {
        try {
            JsonObject json = JsonParser.parseString(jsonResponse).getAsJsonObject();
            String text = json.has("text") ? json.get("text").getAsString().strip().toLowerCase() : "";
            return text;
        } catch (Exception e) {
            System.err.println("[WHISPER] Failed to parse response: " + e.getMessage());
            return "";
        }
    }
}
