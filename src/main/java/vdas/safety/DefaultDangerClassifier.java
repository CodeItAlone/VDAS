package vdas.safety;

import vdas.intent.Intent;

import java.util.Set;

/**
 * Deterministic danger classifier with a hardcoded set of dangerous command
 * names.
 *
 * A command is dangerous if its resolved command name is in the predefined set.
 * Unresolved intents are never classified as dangerous.
 */
public class DefaultDangerClassifier implements DangerClassifier {

    /**
     * Hardcoded set of dangerous command names.
     * These commands affect system state in ways that cannot be easily undone.
     */
    private static final Set<String> DANGEROUS_COMMANDS = Set.of(
            "quit",
            "shutdown",
            "restart",
            "delete",
            "remove",
            "format");

    @Override
    public boolean isDangerous(Intent intent) {
        return intent.getResolvedCommand()
                .map(cmd -> DANGEROUS_COMMANDS.contains(cmd.getName()))
                .orElse(false);
    }
}
