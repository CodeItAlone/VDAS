package vdas.skill;

import org.junit.jupiter.api.Test;
import vdas.intent.Intent;
import vdas.model.SystemCommand;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class SystemInfoSkillTest {

    private final SystemInfoSkill skill = new SystemInfoSkill(new vdas.executor.CommandExecutor());

    @Test
    void canHandle_systemInfo() {
        Intent intent = intentFor("system-info");
        assertTrue(skill.canHandle(intent));
    }

    @Test
    void canHandle_javaVersion() {
        Intent intent = intentFor("java-version");
        assertTrue(skill.canHandle(intent));
    }

    @Test
    void cannotHandle_listFiles() {
        Intent intent = intentFor("list-files");
        assertFalse(skill.canHandle(intent));
    }

    @Test
    void cannotHandle_noResolvedCommand() {
        Intent intent = Intent.forTesting("something", "something", Optional.empty(), 0.0);
        assertFalse(skill.canHandle(intent));
    }

    private Intent intentFor(String commandName) {
        SystemCommand cmd = new SystemCommand(commandName, "dummy", null);
        return Intent.forTesting(commandName, commandName, Optional.of(cmd), 1.0);
    }
}
