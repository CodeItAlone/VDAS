package vdas.safety;

import vdas.intent.AmbiguityDetector;
import vdas.intent.ConfidenceBand;
import vdas.intent.Intent;

/**
 * Central execution gate that determines whether an intent should be
 * executed immediately, require confirmation, require clarification,
 * or be rejected.
 *
 * Decision table:
 * 
 * <pre>
 *   ConfidenceBand | Dangerous | Ambiguous | Decision
 *   ───────────────┼───────────┼───────────┼──────────
 *   (unresolved)   | —         | —         | REJECT
 *   HIGH           | NO        | —         | EXECUTE
 *   HIGH           | YES       | —         | CONFIRM
 *   MEDIUM         | *         | YES       | CLARIFY
 *   MEDIUM         | *         | NO        | CONFIRM
 *   LOW            | *         | *         | REJECT
 * </pre>
 *
 * Unresolved intents are rejected immediately without danger evaluation.
 */
public class ExecutionGate {

    /**
     * Possible decisions for an intent.
     */
    public enum Decision {
        /** Execute the intent immediately. */
        EXECUTE,
        /** Ask the user for confirmation before executing. */
        CONFIRM,
        /** Ask the user to clarify which command they meant. */
        CLARIFY,
        /** Reject the intent — do not execute. */
        REJECT
    }

    private final DangerClassifier dangerClassifier;
    private final AmbiguityDetector ambiguityDetector;

    public ExecutionGate(DangerClassifier dangerClassifier, AmbiguityDetector ambiguityDetector) {
        this.dangerClassifier = dangerClassifier;
        this.ambiguityDetector = ambiguityDetector;
    }

    /**
     * Evaluates the given intent and returns the appropriate decision.
     *
     * @param intent the intent to evaluate
     * @return the execution decision
     */
    public Decision evaluate(Intent intent) {
        // Unresolved intents are always rejected immediately.
        if (intent.getResolvedCommand().isEmpty()) {
            return Decision.REJECT;
        }

        ConfidenceBand band = intent.getConfidenceBand();

        switch (band) {
            case LOW:
                return Decision.REJECT;

            case MEDIUM:
                if (ambiguityDetector.isAmbiguous(intent)) {
                    return Decision.CLARIFY;
                }
                return Decision.CONFIRM;

            case HIGH:
                if (dangerClassifier.isDangerous(intent)) {
                    return Decision.CONFIRM;
                }
                return Decision.EXECUTE;

            default:
                return Decision.REJECT;
        }
    }
}
