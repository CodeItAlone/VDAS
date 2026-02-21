package vdas.safety;

import vdas.intent.Intent;

/**
 * Classifies whether an intent is considered dangerous.
 *
 * A dangerous command requires explicit user confirmation before execution.
 */
public interface DangerClassifier {

    /**
     * Returns true if the given intent is classified as dangerous.
     *
     * @param intent the intent to classify
     * @return true if dangerous, false otherwise
     */
    boolean isDangerous(Intent intent);
}
