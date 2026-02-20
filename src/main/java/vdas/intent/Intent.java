package vdas.intent;

import vdas.model.SystemCommand;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;

/**
 * First-class intent representation.
 *
 * Carries raw + normalized input, an optional resolved command,
 * the confidence score of the resolution, and optional parameters
 * extracted during resolution (e.g. {@code {"app": "chrome"}}).
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
    private final Map<String, String> parameters;

    /**
     * Package-private constructor — only IntentResolver may create Intents.
     */
    Intent(String rawInput, String normalizedInput,
            Optional<SystemCommand> resolvedCommand, double confidence) {
        this(rawInput, normalizedInput, resolvedCommand, confidence, Collections.emptyMap());
    }

    /**
     * Package-private constructor with parameters — only IntentResolver may create
     * Intents.
     */
    Intent(String rawInput, String normalizedInput,
            Optional<SystemCommand> resolvedCommand, double confidence,
            Map<String, String> parameters) {
        this.rawInput = rawInput;
        this.normalizedInput = normalizedInput;
        this.resolvedCommand = resolvedCommand;
        this.confidence = confidence;
        this.parameters = Collections.unmodifiableMap(parameters);
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

    /**
     * Returns an unmodifiable map of parameters extracted during intent resolution.
     * For example, an "open-app" intent may contain {@code {"app": "chrome"}}.
     *
     * @return unmodifiable parameter map (never null, may be empty)
     */
    public Map<String, String> getParameters() {
        return parameters;
    }

    @Override
    public String toString() {
        String base = "Intent{rawInput='" + rawInput + "'"
                + ", normalizedInput='" + normalizedInput + "'"
                + ", resolved=" + resolvedCommand.map(SystemCommand::getName).orElse("none")
                + ", confidence=" + String.format("%.2f", confidence);
        if (!parameters.isEmpty()) {
            base += ", parameters=" + parameters;
        }
        return base + "}";
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

    /**
     * Test-only factory with parameters.
     * <p>
     * DO NOT use in production code — use {@link IntentResolver} instead.
     */
    public static Intent forTesting(String rawInput, String normalizedInput,
            Optional<SystemCommand> resolvedCommand, double confidence,
            Map<String, String> parameters) {
        return new Intent(rawInput, normalizedInput, resolvedCommand, confidence, parameters);
    }
}
