package vdas.intent;

import java.util.Set;

/**
 * Deterministic normalizer that converts speech phrases and command identifiers
 * into a common comparable form.
 *
 * "list files" → "list files"
 * "list-files" → "list files"
 * "Java Version" → "java version"
 * "the system info" → "system info" (leading article stripped)
 * "list the files" → "list the files" (middle article preserved)
 */
public class IntentNormalizer {

    /**
     * Leading stop-words commonly injected by ASR (Vosk).
     * Only removed when they appear as the first token.
     */
    private static final Set<String> LEADING_STOP_WORDS = Set.of("the", "a", "an");

    /**
     * Normalizes user input (voice or keyboard).
     * Lowercases, strips punctuation, collapses whitespace,
     * and removes a leading stop-word if present.
     */
    public static String normalize(String input) {
        if (input == null)
            return "";

        String result = input
                .toLowerCase()
                .trim()
                .replaceAll("[^a-z0-9 ]", "")
                .replaceAll("\\s+", " ");

        return stripLeadingStopWord(result);
    }

    /**
     * Normalizes a stored command name (e.g., "list-files" → "list files").
     * Replaces hyphens/underscores with spaces, then applies standard
     * normalization.
     */
    public static String normalizeCommandName(String name) {
        if (name == null)
            return "";

        return normalize(name.replace("-", " ").replace("_", " "));
    }

    /**
     * Strips a leading stop-word (first token only) if present.
     * "the quit" → "quit", "list the files" → "list the files" (unchanged).
     */
    private static String stripLeadingStopWord(String input) {
        if (input.isEmpty()) {
            return input;
        }

        int spaceIndex = input.indexOf(' ');
        if (spaceIndex == -1) {
            // Single word — only strip if the entire input is a stop-word
            // (e.g. user says just "the") → return empty
            return LEADING_STOP_WORDS.contains(input) ? "" : input;
        }

        String firstToken = input.substring(0, spaceIndex);
        if (LEADING_STOP_WORDS.contains(firstToken)) {
            return input.substring(spaceIndex + 1);
        }

        return input;
    }
}
