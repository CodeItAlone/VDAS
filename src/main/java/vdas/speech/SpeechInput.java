package vdas.speech;

/**
 * Abstraction for speech input sources.
 * Implementations convert spoken audio into plain text strings
 * compatible with the existing command selection logic.
 */
public interface SpeechInput {

    /**
     * Listens to the microphone and returns the recognized text.
     *
     * @return recognized text, or empty string on silence/failure
     */
    String listen();

    /**
     * Releases any held resources (model, recognizer, audio line).
     */
    void close();
}
