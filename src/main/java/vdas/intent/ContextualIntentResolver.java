package vdas.intent;

import vdas.model.SystemCommand;
import vdas.session.SessionContext;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Deterministic follow-up intent resolver using session context.
 *
 * <p>
 * Resolves partial or underspecified commands (e.g. "again", "close it",
 * "open youtube") by looking at what was last successfully executed in
 * the current session.
 * </p>
 *
 * <p>
 * <b>Strategies (evaluated in order):</b>
 * </p>
 * <ol>
 * <li><b>Repeat</b> — "again", "repeat", etc. → re-execute last command</li>
 * <li><b>Close-it</b> — "close it", "close" → close the last-opened app
 * (only if last command was "open-app")</li>
 * <li><b>Contextual navigation</b> — "open youtube" → navigate the
 * last-opened browser to a URL (only if last command was "open-app"
 * and current input is an unresolved "open &lt;target&gt;")</li>
 * </ol>
 *
 * <p>
 * <b>Rules:</b>
 * </p>
 * <ul>
 * <li>Never overrides HIGH-confidence intents</li>
 * <li>Never fabricates commands — only re-uses existing context</li>
 * <li>Never mutates the incoming Intent</li>
 * <li>Context is advisory, never authoritative</li>
 * <li>No NLP, no guessing, no learning. Fully deterministic.</li>
 * </ul>
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

    private static final Set<String> CLOSE_PHRASES = Set.of(
            "close",
            "close it",
            "close that");

    /** The command name that represents app-launch actions. */
    private static final String OPEN_APP_COMMAND = "open-app";

    /** Leading verbs that signal an "open <target>" follow-up. */
    private static final Set<String> NAVIGATION_VERBS = Set.of(
            "open", "launch", "start", "go to", "navigate to");

    /**
     * Whitelisted websites for contextual interpretation instead of local app
     * execution.
     */
    private static final Set<String> WHITELISTED_WEBSITES = Set.of(
            "youtube", "google", "github");

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

        // ── Strategy 0: Context-Aware Website Upgrade ──
        // Intercept already-resolved "open-app" intents where the target is a website,
        // because IntentResolver might have matched "youtube" as a high-confidence app
        // alias.
        if (context.hasContext() && intent.getResolvedCommand().isPresent()) {
            SystemCommand cmd = intent.getResolvedCommand().get();
            if (OPEN_APP_COMMAND.equals(cmd.getName())) {
                String targetApp = intent.getParameters().get("app");
                if (targetApp != null && WHITELISTED_WEBSITES.contains(targetApp.toLowerCase())) {
                    Intent lastIntent = context.getLastIntent();
                    String lastApp = lastIntent.getParameters().get("app");

                    // If the last opened app was a browser
                    if ("chrome".equals(lastApp)) {
                        Map<String, String> newParams = new HashMap<>();
                        newParams.put("app", "chrome");
                        newParams.put("url", targetApp);

                        return Optional.of(Intent.fromContextualFollowUp(
                                intent.getRawInput(), cmd, newParams));
                    }
                }
            }
        }

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

        // ── Strategy 2: Close-it ──
        if (CLOSE_PHRASES.contains(normalized)) {
            return resolveCloseIt(intent, context);
        }

        // ── Strategy 3: Contextual navigation ──
        // Only when the intent is unresolved (LOW confidence, no command)
        if (intent.getResolvedCommand().isEmpty()) {
            return resolveContextualNavigation(intent, context, normalized);
        }

        return Optional.empty();
    }

    /**
     * Resolves "close it" / "close that" / "close" by closing the
     * last-opened app — but ONLY if the last command was "open-app".
     *
     * <p>
     * Never fabricates a close command for non-app contexts.
     * </p>
     */
    private Optional<Intent> resolveCloseIt(Intent intent, SessionContext context) {
        SystemCommand lastCommand = context.getLastCommand();
        Intent lastIntent = context.getLastIntent();

        // Only valid if the last executed command was "open-app"
        if (lastCommand == null || !OPEN_APP_COMMAND.equals(lastCommand.getName())) {
            return Optional.empty();
        }

        // Need the app parameter from the last intent
        String appName = lastIntent.getParameters().get("app");
        if (appName == null || appName.isEmpty()) {
            return Optional.empty();
        }

        // Build parameters for the close action
        Map<String, String> params = new HashMap<>();
        params.put("app", appName);
        params.put("action", "close");

        Intent followUp = Intent.fromContextualFollowUp(
                intent.getRawInput(), lastCommand, params);

        return Optional.of(followUp);
    }

    /**
     * Resolves contextual navigation like "open youtube" when the last
     * command was "open-app" (e.g., the user just opened Chrome).
     *
     * <p>
     * Extracts the target from "open &lt;target&gt;" and creates a follow-up
     * intent that re-uses the last app with an added "url" parameter.
     * </p>
     *
     * <p>
     * No NLP, no guessing — just deterministic verb-prefix stripping
     * and context re-use.
     * </p>
     */
    private Optional<Intent> resolveContextualNavigation(
            Intent intent, SessionContext context, String normalized) {

        SystemCommand lastCommand = context.getLastCommand();
        Intent lastIntent = context.getLastIntent();

        // Only valid if the last command was "open-app"
        if (lastCommand == null || !OPEN_APP_COMMAND.equals(lastCommand.getName())) {
            return Optional.empty();
        }

        // Need the app parameter from the last intent
        String lastApp = lastIntent.getParameters().get("app");
        if (lastApp == null || lastApp.isEmpty()) {
            return Optional.empty();
        }

        // Extract the target from "open <target>" / "go to <target>"
        String target = extractNavigationTarget(normalized);
        if (target.isEmpty()) {
            return Optional.empty();
        }

        // Build parameters: re-use last app, add URL target
        Map<String, String> params = new HashMap<>();
        params.put("app", lastApp);
        params.put("url", target);

        Intent followUp = Intent.fromContextualFollowUp(
                intent.getRawInput(), lastCommand, params);

        return Optional.of(followUp);
    }

    /**
     * Strips a leading navigation verb from the input and returns the target.
     * "open youtube" → "youtube", "go to gmail" → "gmail".
     * Returns empty string if no navigation verb is found.
     */
    private String extractNavigationTarget(String normalized) {
        for (String verb : NAVIGATION_VERBS) {
            if (normalized.startsWith(verb + " ")) {
                String target = normalized.substring(verb.length()).trim();
                if (!target.isEmpty()) {
                    return target;
                }
            }
        }
        return "";
    }
}
