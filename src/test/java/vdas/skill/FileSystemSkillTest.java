package vdas.skill;

import org.junit.jupiter.api.Test;
import vdas.intent.Intent;
import vdas.model.SystemCommand;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class FileSystemSkillTest {

    private final FileSystemSkill skill = new FileSystemSkill(new vdas.executor.CommandExecutor());

    @Test
    void canHandle_listFiles() {
        Intent intent = intentFor("list-files");
        assertTrue(skill.canHandle(intent));
    }

    @Test
    void cannotHandle_systemInfo() {
        Intent intent = intentFor("system-info");
        assertFalse(skill.canHandle(intent));
    }

    @Test
    void cannotHandle_javaVersion() {
        Intent intent = intentFor("java-version");
        assertFalse(skill.canHandle(intent));
    }

    @Test
    void cannotHandle_noResolvedCommand() {
        Intent intent = Intent.forTesting("anything", "anything", Optional.empty(), 0.0);
        assertFalse(skill.canHandle(intent));
    }

    private Intent intentFor(String commandName) {
        SystemCommand cmd = new SystemCommand(commandName, "dummy", null);
        return Intent.forTesting(commandName, commandName, Optional.of(cmd), 1.0);
    }
}
