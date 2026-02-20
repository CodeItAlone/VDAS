package vdas.intent;

import vdas.model.SystemCommand;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Deterministic intent resolution engine.
 *
 * Resolves raw user input (voice or keyboard) to an {@link Intent}
 * using a strict pipeline: exact match → alias match → fuzzy match.
 *
 * When the resolved command is "open-app", the resolver also extracts
 * structured parameters (e.g. {@code {"app": "chrome"}}) from the
 * normalized input, so that downstream skills never parse raw text.
 *
 * No AI, ML, or NLP libraries. Fully offline and auditable.
 */
public class IntentResolver {

    private static final double DEFAULT_THRESHOLD = 0.75;
    private static final double AMBIGUITY_MARGIN = 0.05;

    /** Command name that triggers app-launch parameter extraction. */
    private static final String OPEN_APP_COMMAND = "open-app";

    /** Leading verbs that are stripped to extract the app name. */
    private static final Set<String> APP_LAUNCH_VERBS = Set.of("open", "launch", "start", "run");

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
     * Resolves raw user input to a fully constructed Intent.
     *
     * Pipeline (short-circuits on first match):
     * 1. Normalize input
     * 2. Exact match against normalized command names → confidence 1.0
     * 3. Alias match against normalized aliases → confidence 1.0
     * 4. Fuzzy match (Levenshtein) with score ≥ threshold → confidence = score
     *
     * If no match is found, returns an Intent with empty resolvedCommand
     * and confidence 0.0.
     *
     * When the resolved command is "open-app", the resolver extracts
     * structured parameters (e.g. {@code {"app": "chrome"}}).
     *
     * @param rawInput the raw user input (voice transcription or keyboard)
     * @return a fully constructed Intent (never null)
     */
    public Intent resolve(String rawInput) {
        if (rawInput == null || rawInput.isBlank()) {
            String safe = (rawInput == null) ? "" : rawInput;
            return new Intent(safe, "", Optional.empty(), 0.0);
        }

        String normalized = IntentNormalizer.normalize(rawInput);
        if (normalized.isEmpty()) {
            return new Intent(rawInput, normalized, Optional.empty(), 0.0);
        }

        // ── Step 1: Exact match ──
        for (SystemCommand cmd : commands) {
            String normalizedName = IntentNormalizer.normalizeCommandName(cmd.getName());
            if (normalizedName.equals(normalized)) {
                System.out.println("[INTENT] Exact match: \"" + rawInput + "\" → " + cmd.getName());
                return buildIntent(rawInput, normalized, cmd, 1.0);
            }
        }

        // ── Step 2: Alias match ──
        for (SystemCommand cmd : commands) {
            for (String alias : cmd.getAliases()) {
                if (IntentNormalizer.normalize(alias).equals(normalized)) {
                    System.out.println("[INTENT] Alias match: \"" + rawInput + "\" → " + cmd.getName()
                            + " (alias: \"" + alias + "\")");
                    return buildIntent(rawInput, normalized, cmd, 1.0);
                }
            }
        }

        // ── Step 3: Verb-prefix match for app-launch commands ──
        // Handles "open chrome", "launch vscode" etc. where the app name
        // cannot be enumerated in aliases.
        Intent verbPrefixResult = tryVerbPrefixMatch(normalized, rawInput);
        if (verbPrefixResult != null) {
            return verbPrefixResult;
        }

        // ── Step 4: Fuzzy match ──
        return fuzzyMatch(normalized, rawInput);
    }

    /**
     * Wraps an already-known command into a proper Intent with confidence 1.0.
     * Used when the command is selected by index (numeric input) and does not
     * need the resolution pipeline.
     *
     * @param rawInput the raw user input (e.g. "2")
     * @param cmd      the command selected by index
     * @return a fully constructed Intent with confidence 1.0
     */
    public Intent resolveByCommand(String rawInput, SystemCommand cmd) {
        String normalized = IntentNormalizer.normalize(rawInput);
        return buildIntent(rawInput, normalized, cmd, 1.0);
    }

    /**
     * Builds an Intent, extracting structured parameters when applicable.
     * For "open-app" commands, the app name is extracted from the normalized input.
     */
    private Intent buildIntent(String rawInput, String normalized, SystemCommand cmd, double confidence) {
        Map<String, String> parameters = extractParameters(cmd, normalized);
        return new Intent(rawInput, normalized, Optional.of(cmd), confidence, parameters);
    }

    /**
     * Attempts verb-prefix matching for app-launch commands.
     *
     * If the normalized input starts with a known app-launch verb (open, launch,
     * start, run) followed by a non-empty target, and an "open-app" command exists
     * in the command list, resolves directly with confidence 1.0.
     *
     * This handles phrases like "open chrome" or "launch vscode" where the app
     * name cannot be pre-enumerated in aliases.
     *
     * @return matched Intent, or null if no verb-prefix match
     */
    private Intent tryVerbPrefixMatch(String normalized, String rawInput) {
        String appName = extractAppName(normalized);
        if (appName.isEmpty()) {
            return null;
        }

        // Find the open-app command
        for (SystemCommand cmd : commands) {
            if (OPEN_APP_COMMAND.equals(cmd.getName())) {
                System.out.println("[INTENT] Verb-prefix match: \"" + rawInput + "\" → " + cmd.getName()
                        + " (app: \"" + appName + "\")");
                return buildIntent(rawInput, normalized, cmd, 1.0);
            }
        }

        return null;
    }

    /**
     * Extracts structured parameters from the normalized input based on command
     * type.
     *
     * For "open-app": strips the leading verb (open/launch/start/run) and puts
     * the remainder as {@code {"app": "<appName>"}}.
     *
     * @return parameter map (may be empty)
     */
    private Map<String, String> extractParameters(SystemCommand cmd, String normalized) {
        if (!OPEN_APP_COMMAND.equals(cmd.getName())) {
            return Map.of();
        }

        String appName = extractAppName(normalized);
        if (appName.isEmpty()) {
            return Map.of();
        }

        Map<String, String> params = new HashMap<>();
        params.put("app", appName);
        return params;
    }

    /**
     * Strips the leading verb token from the normalized input to extract the app
     * name.
     * "open chrome" → "chrome", "launch vscode" → "vscode".
     */
    private String extractAppName(String normalized) {
        int spaceIndex = normalized.indexOf(' ');
        if (spaceIndex == -1) {
            return "";
        }

        String firstToken = normalized.substring(0, spaceIndex);
        if (APP_LAUNCH_VERBS.contains(firstToken)) {
            return normalized.substring(spaceIndex + 1).trim();
        }

        // If the input matched via alias like "open app" → normalized might be "open
        // app"
        // In this case there's no app name in the input itself
        return "";
    }

    /**
     * Finds the best fuzzy match across all command names and aliases.
     * Rejects if:
     * - Best score is below the threshold
     * - Top two candidates are within AMBIGUITY_MARGIN (ambiguous)
     */
    private Intent fuzzyMatch(String normalized, String rawInput) {
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
            return new Intent(rawInput, normalized, Optional.empty(), bestScore);
        }

        // ── Ambiguity gate ──
        if (secondCommand != null
                && secondCommand != bestCommand
                && (bestScore - secondScore) < AMBIGUITY_MARGIN) {
            System.out.println("[INTENT] Ambiguous: \"" + rawInput
                    + "\" (top: " + bestCommand.getName() + " @ " + String.format("%.2f", bestScore)
                    + ", runner-up: " + secondCommand.getName() + " @ " + String.format("%.2f", secondScore) + ")");
            return new Intent(rawInput, normalized, Optional.empty(), bestScore);
        }

        System.out.println("[INTENT] Fuzzy match: \"" + rawInput + "\" → " + bestCommand.getName()
                + " (score: " + String.format("%.2f", bestScore)
                + ", matched: \"" + bestTarget + "\")");
        return buildIntent(rawInput, normalized, bestCommand, bestScore);
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
