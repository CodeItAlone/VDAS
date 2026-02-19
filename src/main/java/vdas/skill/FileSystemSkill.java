package vdas.skill;

import vdas.executor.CommandExecutor;
import vdas.intent.Intent;

import java.util.Set;

/**
 * Skill for file system commands: list-files.
 * Stateless — delegates execution to CommandExecutor.
 */
public class FileSystemSkill implements Skill {

    private static final Set<String> SUPPORTED_COMMANDS = Set.of("list-files");

    private final CommandExecutor executor;

    public FileSystemSkill(CommandExecutor executor) {
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
