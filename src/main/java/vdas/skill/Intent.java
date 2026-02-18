package vdas.skill;

import vdas.model.SystemCommand;

import java.util.Optional;

/**
 * Decoupled intent representation.
 *
 * Carries raw + normalized input and an optional resolved command.
 * Skills inspect this to decide canHandle without assuming resolution is final.
 */
public class Intent {

    private final String rawInput;
    private final String normalizedInput;
    private final Optional<SystemCommand> resolvedCommand;

    public Intent(String rawInput, String normalizedInput, Optional<SystemCommand> resolvedCommand) {
        this.rawInput = rawInput;
        this.normalizedInput = normalizedInput;
        this.resolvedCommand = resolvedCommand;
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

    @Override
    public String toString() {
        return "Intent{rawInput='" + rawInput + "'"
                + ", normalizedInput='" + normalizedInput + "'"
                + ", resolved=" + resolvedCommand.map(c -> c.getName()).orElse("none")
                + "}";
    }
}
