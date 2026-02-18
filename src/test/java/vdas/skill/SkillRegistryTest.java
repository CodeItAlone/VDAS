package vdas.skill;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import vdas.executor.CommandExecutor;
import vdas.model.SystemCommand;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class SkillRegistryTest {

    private SkillRegistry registry;

    @BeforeEach
    void setUp() {
        CommandExecutor executor = new CommandExecutor();
        registry = new SkillRegistry(List.of(
                new SystemInfoSkill(executor),
                new FileSystemSkill(executor)));
    }

    @Test
    void findsSystemInfoSkill_forSystemInfo() {
        Intent intent = intentFor("system-info");
        Optional<Skill> skill = registry.findSkill(intent);
        assertTrue(skill.isPresent());
        assertInstanceOf(SystemInfoSkill.class, skill.get());
    }

    @Test
    void findsSystemInfoSkill_forJavaVersion() {
        Intent intent = intentFor("java-version");
        Optional<Skill> skill = registry.findSkill(intent);
        assertTrue(skill.isPresent());
        assertInstanceOf(SystemInfoSkill.class, skill.get());
    }

    @Test
    void findsFileSystemSkill_forListFiles() {
        Intent intent = intentFor("list-files");
        Optional<Skill> skill = registry.findSkill(intent);
        assertTrue(skill.isPresent());
        assertInstanceOf(FileSystemSkill.class, skill.get());
    }

    @Test
    void returnsEmpty_forUnknownCommand() {
        Intent intent = intentFor("unknown-command");
        Optional<Skill> skill = registry.findSkill(intent);
        assertTrue(skill.isEmpty());
    }

    @Test
    void returnsEmpty_whenNoResolvedCommand() {
        Intent intent = new Intent("anything", "anything", Optional.empty());
        Optional<Skill> skill = registry.findSkill(intent);
        assertTrue(skill.isEmpty());
    }

    @Test
    void firstMatchWins_orderMatters() {
        // Register two skills that both claim "system-info"
        CommandExecutor executor = new CommandExecutor();
        Skill first = new SystemInfoSkill(executor);
        Skill second = new SystemInfoSkill(executor); // duplicate
        SkillRegistry ordered = new SkillRegistry(List.of(first, second));

        Intent intent = intentFor("system-info");
        Optional<Skill> skill = ordered.findSkill(intent);
        assertTrue(skill.isPresent());
        assertSame(first, skill.get(), "First registered skill should win");
    }

    private Intent intentFor(String commandName) {
        SystemCommand cmd = new SystemCommand(commandName, "dummy", null);
        return new Intent(commandName, commandName, Optional.of(cmd));
    }
}
