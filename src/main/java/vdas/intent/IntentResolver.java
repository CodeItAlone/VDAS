package vdas.intent;

import vdas.model.SystemCommand;

import java.util.List;
import java.util.Optional;

/**
 * Deterministic intent resolution engine.
 *
 * Resolves raw user input (voice or keyboard) to a SystemCommand
 * using a strict pipeline: exact match → alias match → fuzzy match.
 *
 * No AI, ML, or NLP libraries. Fully offline and auditable.
 */
public class IntentResolver {

    private static final double DEFAULT_THRESHOLD = 0.75;
    private static final double AMBIGUITY_MARGIN = 0.05;

    private final List<SystemCommand> commands;
    private final double threshold;

    public IntentResolver(List<SystemCommand> commands) {
        this(commands, DEFAULT_THRESHOLD);
    }

    public IntentResolver(List<SystemCommand> commands, double threshold) {
        if (commands == null || commands.isEmpty()) {
            throw new IllegalArgumentException("Commands list must not be null or empty");
        }
        if (threshold < 0.0 || threshold > 1.0) {
            throw new IllegalArgumentException("Threshold must be between 0.0 and 1.0");
        }
        this.commands = commands;
        this.threshold = threshold;
    }

    /**
     * Resolves raw user input to a SystemCommand.
     *
     * Pipeline (short-circuits on first match):
     * 1. Normalize input
     * 2. Exact match against normalized command names
     * 3. Alias match against normalized aliases
     * 4. Fuzzy match (Levenshtein) with confidence ≥ threshold
     *
     * @param rawInput the raw user input (voice transcription or keyboard)
     * @return the resolved command, or empty if no confident match
     */
    public Optional<SystemCommand> resolve(String rawInput) {
        if (rawInput == null || rawInput.isBlank()) {
            return Optional.empty();
        }

        String normalized = IntentNormalizer.normalize(rawInput);
        if (normalized.isEmpty()) {
            return Optional.empty();
        }

        // ── Step 1: Exact match ──
        for (SystemCommand cmd : commands) {
            String normalizedName = IntentNormalizer.normalizeCommandName(cmd.getName());
            if (normalizedName.equals(normalized)) {
                System.out.println("[INTENT] Exact match: \"" + rawInput + "\" → " + cmd.getName());
                return Optional.of(cmd);
            }
        }

        // ── Step 2: Alias match ──
        for (SystemCommand cmd : commands) {
            for (String alias : cmd.getAliases()) {
                if (IntentNormalizer.normalize(alias).equals(normalized)) {
                    System.out.println("[INTENT] Alias match: \"" + rawInput + "\" → " + cmd.getName()
                            + " (alias: \"" + alias + "\")");
                    return Optional.of(cmd);
                }
            }
        }

        // ── Step 3: Fuzzy match ──
        return fuzzyMatch(normalized, rawInput);
    }

    /**
     * Finds the best fuzzy match across all command names and aliases.
     * Rejects if:
     * - Best score is below the threshold
     * - Top two candidates are within AMBIGUITY_MARGIN (ambiguous)
     */
    private Optional<SystemCommand> fuzzyMatch(String normalized, String rawInput) {
        SystemCommand bestCommand = null;
        double bestScore = 0.0;
        String bestTarget = "";

        SystemCommand secondCommand = null;
        double secondScore = 0.0;

        for (SystemCommand cmd : commands) {
            // Check against command name
            String normalizedName = IntentNormalizer.normalizeCommandName(cmd.getName());
            double score = LevenshteinDistance.similarity(normalized, normalizedName);
            FuzzyCandidate result = updateBest(cmd, score, normalizedName,
                    bestCommand, bestScore, bestTarget, secondCommand, secondScore);
            bestCommand = result.bestCommand;
            bestScore = result.bestScore;
            bestTarget = result.bestTarget;
            secondCommand = result.secondCommand;
            secondScore = result.secondScore;

            // Check against each alias
            for (String alias : cmd.getAliases()) {
                String normalizedAlias = IntentNormalizer.normalize(alias);
                score = LevenshteinDistance.similarity(normalized, normalizedAlias);
                result = updateBest(cmd, score, normalizedAlias,
                        bestCommand, bestScore, bestTarget, secondCommand, secondScore);
                bestCommand = result.bestCommand;
                bestScore = result.bestScore;
                bestTarget = result.bestTarget;
                secondCommand = result.secondCommand;
                secondScore = result.secondScore;
            }
        }

        // ── Confidence gate ──
        if (bestScore < threshold) {
            System.out.println("[INTENT] Rejected: \"" + rawInput
                    + "\" (best score: " + String.format("%.2f", bestScore)
                    + " < threshold: " + String.format("%.2f", threshold) + ")");
            return Optional.empty();
        }

        // ── Ambiguity gate ──
        if (secondCommand != null
                && secondCommand != bestCommand
                && (bestScore - secondScore) < AMBIGUITY_MARGIN) {
            System.out.println("[INTENT] Ambiguous: \"" + rawInput
                    + "\" (top: " + bestCommand.getName() + " @ " + String.format("%.2f", bestScore)
                    + ", runner-up: " + secondCommand.getName() + " @ " + String.format("%.2f", secondScore) + ")");
            return Optional.empty();
        }

        System.out.println("[INTENT] Fuzzy match: \"" + rawInput + "\" → " + bestCommand.getName()
                + " (score: " + String.format("%.2f", bestScore)
                + ", matched: \"" + bestTarget + "\")");
        return Optional.of(bestCommand);
    }

    /**
     * Helper to track top-two fuzzy candidates.
     */
    private FuzzyCandidate updateBest(SystemCommand cmd, double score, String target,
            SystemCommand bestCmd, double bestScore, String bestTarget,
            SystemCommand secondCmd, double secondScore) {
        if (score > bestScore) {
            // Demote current best to second (only if different command)
            if (bestCmd != null && bestCmd != cmd) {
                secondCmd = bestCmd;
                secondScore = bestScore;
            }
            return new FuzzyCandidate(cmd, score, target, secondCmd, secondScore);
        } else if (score > secondScore && cmd != bestCmd) {
            return new FuzzyCandidate(bestCmd, bestScore, bestTarget, cmd, score);
        }
        return new FuzzyCandidate(bestCmd, bestScore, bestTarget, secondCmd, secondScore);
    }

    /**
     * Internal value holder for fuzzy matching state.
     */
    private static class FuzzyCandidate {
        final SystemCommand bestCommand;
        final double bestScore;
        final String bestTarget;
        final SystemCommand secondCommand;
        final double secondScore;

        FuzzyCandidate(SystemCommand bestCommand, double bestScore, String bestTarget,
                SystemCommand secondCommand, double secondScore) {
            this.bestCommand = bestCommand;
            this.bestScore = bestScore;
            this.bestTarget = bestTarget;
            this.secondCommand = secondCommand;
            this.secondScore = secondScore;
        }
    }
}
