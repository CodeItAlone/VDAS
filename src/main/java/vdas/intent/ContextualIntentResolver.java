package vdas.intent;

import vdas.session.SessionContext;

import java.util.Optional;
import java.util.Set;

/**
 * Deterministic follow-up intent resolver using session context.
 *
 * <p>
 * Resolves partial or underspecified commands (e.g. "again", "repeat")
 * by looking at what was last successfully executed in the current session.
 * </p>
 *
 * <p>
 * <b>Rules:</b>
 * </p>
 * <ul>
 * <li>Never overrides HIGH-confidence intents</li>
 * <li>Never fabricates commands — only re-uses existing context</li>
 * <li>Never mutates the incoming Intent</li>
 * <li>Context is advisory, never authoritative</li>
 * </ul>
 *
 * <p>
 * No NLP, no guessing, no learning. Fully deterministic.
 * </p>
 */
public class ContextualIntentResolver {

    private static final Set<String> REPEAT_PHRASES = Set.of(
            "again",
            "repeat",
            "do it again",
            "do that again",
            "same",
            "one more time",
            "repeat that",
            "run it again");

    /**
     * Attempts to resolve the given intent using the session context.
     *
     * <p>
     * Returns a NEW Intent if a contextual match is found.
     * Returns empty if the intent should proceed through normal resolution.
     * </p>
     *
     * @param intent  the current (possibly unresolved) intent
     * @param context the session context from previous executions
     * @return a contextually resolved Intent, or empty
     */
    public Optional<Intent> resolve(Intent intent, SessionContext context) {

        // ── Guard rules (strict, ordered) ──

        // 1. Never override HIGH confidence intents
        if (intent.getConfidenceBand() == ConfidenceBand.HIGH) {
            return Optional.empty();
        }

        // 2. Need context to resolve
        if (!context.hasContext()) {
            return Optional.empty();
        }

        // 3. Don't second-guess already-resolved MEDIUM+ intents
        if (intent.getResolvedCommand().isPresent()
                && intent.getConfidenceBand().isAtLeast(ConfidenceBand.MEDIUM)) {
            return Optional.empty();
        }

        // ── Normalize input ──
        String normalized = intent.getRawInput()
                .trim()
                .toLowerCase()
                .replaceAll("\\s+", " ");

        // ── Strategy 1: Repeat ──
        if (REPEAT_PHRASES.contains(normalized)) {

            Intent lastIntent = context.getLastIntent();

            if (lastIntent == null) {
                return Optional.empty();
            }
            if (lastIntent.getResolvedCommand().isEmpty()) {
                return Optional.empty();
            }

            // Create a NEW intent — never mutate the incoming one
            Intent repeated = Intent.fromContextualRepeat(
                    intent.getRawInput(),
                    lastIntent.getResolvedCommand().get(),
                    lastIntent.getParameters());

            return Optional.of(repeated);
        }

        return Optional.empty();
    }
}
