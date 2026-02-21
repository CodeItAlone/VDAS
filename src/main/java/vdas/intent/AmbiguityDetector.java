package vdas.intent;

/**
 * Detects whether an intent is ambiguous.
 *
 * Ambiguity means multiple commands scored similarly during fuzzy matching,
 * and the system cannot confidently determine which command the user intended.
 */
public interface AmbiguityDetector {

    /**
     * Returns true if the given intent is ambiguous.
     *
     * @param intent the intent to evaluate
     * @return true if ambiguous, false otherwise
     */
    boolean isAmbiguous(Intent intent);
}
