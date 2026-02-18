package vdas.skill;

import vdas.executor.CommandExecutor;

import java.util.Set;

/**
 * Skill for system information commands: system-info, java-version.
 * Stateless — delegates execution to CommandExecutor.
 */
public class SystemInfoSkill implements Skill {

    private static final Set<String> SUPPORTED_COMMANDS = Set.of("system-info", "java-version");

    private final CommandExecutor executor;

    public SystemInfoSkill(CommandExecutor executor) {
        this.executor = executor;
    }

    @Override
    public boolean canHandle(Intent intent) {
        return intent.getResolvedCommand()
                .map(cmd -> SUPPORTED_COMMANDS.contains(cmd.getName()))
                .orElse(false);
    }

    @Override
    public void execute(Intent intent) {
        intent.getResolvedCommand().ifPresent(executor::execute);
    }
}
