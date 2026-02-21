package vdas.safety;

import vdas.intent.ConfidenceBand;
import vdas.intent.Intent;

/**
 * Central execution gate that determines whether an intent should be
 * executed immediately, require confirmation, or be rejected.
 *
 * Decision table (from PRD):
 * 
 * <pre>
 *   ConfidenceBand | Dangerous | Decision
 *   ───────────────┼───────────┼──────────
 *   (unresolved)   | —         | REJECT
 *   HIGH           | NO        | EXECUTE
 *   HIGH           | YES       | CONFIRM
 *   MEDIUM         | *         | CONFIRM
 *   LOW            | *         | REJECT
 * </pre>
 *
 * Unresolved intents are rejected immediately without danger evaluation
 * (Refinement 2).
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
        /** Reject the intent — do not execute. */
        REJECT
    }

    private final DangerClassifier dangerClassifier;

    public ExecutionGate(DangerClassifier dangerClassifier) {
        this.dangerClassifier = dangerClassifier;
    }

    /**
     * Evaluates the given intent and returns the appropriate decision.
     *
     * @param intent the intent to evaluate
     * @return the execution decision
     */
    public Decision evaluate(Intent intent) {
        // Refinement 2: Unresolved intents are always rejected immediately.
        // Do not call DangerClassifier for unresolved intents.
        if (intent.getResolvedCommand().isEmpty()) {
            return Decision.REJECT;
        }

        ConfidenceBand band = intent.getConfidenceBand();

        switch (band) {
            case LOW:
                return Decision.REJECT;

            case MEDIUM:
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
