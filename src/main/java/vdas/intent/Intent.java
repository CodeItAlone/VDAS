package vdas.intent;

import vdas.model.SystemCommand;

import java.util.Optional;

/**
 * First-class intent representation.
 *
 * Carries raw + normalized input, an optional resolved command,
 * and the confidence score of the resolution.
 *
 * Immutable — all fields are final, no setters.
 * Only {@link IntentResolver} may construct instances (package-private
 * constructor).
 */
public final class Intent {

    private final String rawInput;
    private final String normalizedInput;
    private final Optional<SystemCommand> resolvedCommand;
    private final double confidence;

    /**
     * Package-private constructor — only IntentResolver may create Intents.
     */
    Intent(String rawInput, String normalizedInput,
            Optional<SystemCommand> resolvedCommand, double confidence) {
        this.rawInput = rawInput;
        this.normalizedInput = normalizedInput;
        this.resolvedCommand = resolvedCommand;
        this.confidence = confidence;
    }

    public String getRawInput() {
        return rawInput;
    }

    public String getNormalizedInput() {
        return normalizedInput;
    }

    public Optional<SystemCommand> getResolvedCommand() {
        return resolvedCommand;
    }

    public double getConfidence() {
        return confidence;
    }

    @Override
    public String toString() {
        return "Intent{rawInput='" + rawInput + "'"
                + ", normalizedInput='" + normalizedInput + "'"
                + ", resolved=" + resolvedCommand.map(SystemCommand::getName).orElse("none")
                + ", confidence=" + String.format("%.2f", confidence)
                + "}";
    }

    /**
     * Test-only factory. Allows test code outside the vdas.intent package
     * to construct Intent instances for unit testing.
     * <p>
     * DO NOT use in production code — use {@link IntentResolver} instead.
     */
    public static Intent forTesting(String rawInput, String normalizedInput,
            Optional<SystemCommand> resolvedCommand, double confidence) {
        return new Intent(rawInput, normalizedInput, resolvedCommand, confidence);
    }
}
