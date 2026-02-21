package vdas.intent;

import vdas.model.SystemCommand;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * First-class intent representation.
 *
 * Carries raw + normalized input, an optional resolved command,
 * the confidence score of the resolution, optional parameters
 * extracted during resolution, and optional candidate commands
 * for ambiguity clarification.
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
    private final ConfidenceBand confidenceBand;
    private final Map<String, String> parameters;
    private final List<SystemCommand> candidateCommands;
    private final List<Double> candidateScores;

    /**
     * Package-private constructor — only IntentResolver may create Intents.
     */
    Intent(String rawInput, String normalizedInput,
            Optional<SystemCommand> resolvedCommand, double confidence) {
        this(rawInput, normalizedInput, resolvedCommand, confidence,
                Collections.emptyMap(), Collections.emptyList(), Collections.emptyList());
    }

    /**
     * Package-private constructor with parameters — only IntentResolver may create
     * Intents.
     */
    Intent(String rawInput, String normalizedInput,
            Optional<SystemCommand> resolvedCommand, double confidence,
            Map<String, String> parameters) {
        this(rawInput, normalizedInput, resolvedCommand, confidence,
                parameters, Collections.emptyList(), Collections.emptyList());
    }

    /**
     * Package-private full constructor with parameters and candidates.
     */
    Intent(String rawInput, String normalizedInput,
            Optional<SystemCommand> resolvedCommand, double confidence,
            Map<String, String> parameters,
            List<SystemCommand> candidateCommands,
            List<Double> candidateScores) {
        this.rawInput = rawInput;
        this.normalizedInput = normalizedInput;
        this.resolvedCommand = resolvedCommand;
        this.confidence = confidence;
        this.confidenceBand = ConfidenceBand.of(confidence);
        this.parameters = Collections.unmodifiableMap(parameters);
        this.candidateCommands = List.copyOf(candidateCommands);
        this.candidateScores = List.copyOf(candidateScores);
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
     * Returns the confidence band assigned during construction.
     * This is the single source of truth — never recomputed downstream.
     */
    public ConfidenceBand getConfidenceBand() {
        return confidenceBand;
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

    /**
     * Returns the list of candidate commands, sorted by descending confidence
     * score.
     * Non-empty only when the intent is ambiguous (multiple close-scoring fuzzy
     * matches).
     *
     * @return unmodifiable candidate list (never null, may be empty)
     */
    public List<SystemCommand> getCandidateCommands() {
        return candidateCommands;
    }

    /**
     * Returns the confidence scores parallel to {@link #getCandidateCommands()}.
     * Same ordering — index 0 is the score for candidate 0, etc.
     *
     * @return unmodifiable score list (never null, may be empty)
     */
    public List<Double> getCandidateScores() {
        return candidateScores;
    }

    /**
     * Returns a new Intent with the given resolved command at confidence 1.0 (HIGH
     * band).
     * Used after clarification to create a clarified intent that can be re-gated.
     * Preserves raw and normalized input from the original intent.
     *
     * @param command the clarified command
     * @return a new immutable Intent with the clarified command
     */
    public Intent withResolvedCommand(SystemCommand command) {
        return new Intent(rawInput, normalizedInput,
                Optional.of(command), 1.0, parameters);
    }

    @Override
    public String toString() {
        String base = "Intent{rawInput='" + rawInput + "'"
                + ", normalizedInput='" + normalizedInput + "'"
                + ", resolved=" + resolvedCommand.map(SystemCommand::getName).orElse("none")
                + ", confidence=" + String.format("%.2f", confidence)
                + ", band=" + confidenceBand;
        if (!parameters.isEmpty()) {
            base += ", parameters=" + parameters;
        }
        if (!candidateCommands.isEmpty()) {
            base += ", candidates=" + candidateCommands.size();
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

    /**
     * Test-only factory with candidates.
     * <p>
     * DO NOT use in production code — use {@link IntentResolver} instead.
     */
    public static Intent forTesting(String rawInput, String normalizedInput,
            Optional<SystemCommand> resolvedCommand, double confidence,
            List<SystemCommand> candidateCommands, List<Double> candidateScores) {
        return new Intent(rawInput, normalizedInput, resolvedCommand, confidence,
                Collections.emptyMap(), candidateCommands, candidateScores);
    }
}
