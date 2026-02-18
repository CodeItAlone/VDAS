package vdas.skill;

import org.junit.jupiter.api.Test;
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
        Intent intent = new Intent("anything", "anything", Optional.empty());
        assertFalse(skill.canHandle(intent));
    }

    private Intent intentFor(String commandName) {
        SystemCommand cmd = new SystemCommand(commandName, "dummy", null);
        return new Intent(commandName, commandName, Optional.of(cmd));
    }
}
