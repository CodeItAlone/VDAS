package vdas.intent;

/**
 * Deterministic normalizer that converts speech phrases and command identifiers
 * into a common comparable form.
 *
 * "list files" → "list files"
 * "list-files" → "list files"
 * "Java Version" → "java version"
 */
public class IntentNormalizer {

    /**
     * Normalizes user input (voice or keyboard).
     * Lowercases, strips punctuation, collapses whitespace.
     */
    public static String normalize(String input) {
        if (input == null)
            return "";

        return input
                .toLowerCase()
                .trim()
                .replaceAll("[^a-z0-9 ]", "")
                .replaceAll("\\s+", " ");
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
}
