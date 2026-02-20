package vdas.skill;

import org.junit.jupiter.api.Test;
import vdas.intent.Intent;
import vdas.model.SystemCommand;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class AppLauncherSkillTest {

    private final AppLauncherSkill skill = new AppLauncherSkill();

    // ── canHandle: whitelisted apps ──

    @Test
    void canHandle_openChrome() {
        Intent intent = appIntent("chrome");
        assertTrue(skill.canHandle(intent));
    }

    @Test
    void canHandle_launchVscode() {
        Intent intent = appIntent("vscode");
        assertTrue(skill.canHandle(intent));
    }

    @Test
    void canHandle_openExplorer() {
        Intent intent = appIntent("explorer");
        assertTrue(skill.canHandle(intent));
    }

    // ── canHandle: rejections ──

    @Test
    void cannotHandle_unknownApp() {
        Intent intent = appIntent("unknownapp");
        assertFalse(skill.canHandle(intent));
    }

    @Test
    void cannotHandle_noResolvedCommand() {
        Intent intent = Intent.forTesting("open chrome", "open chrome",
                Optional.empty(), 0.0, Map.of("app", "chrome"));
        assertFalse(skill.canHandle(intent));
    }

    @Test
    void cannotHandle_differentCommand() {
        SystemCommand cmd = new SystemCommand("system-info", "systeminfo", null);
        Intent intent = Intent.forTesting("system info", "system info",
                Optional.of(cmd), 1.0);
        assertFalse(skill.canHandle(intent));
    }

    @Test
    void cannotHandle_noAppParam() {
        SystemCommand cmd = new SystemCommand("open-app", "", null);
        Intent intent = Intent.forTesting("open", "open",
                Optional.of(cmd), 1.0);
        assertFalse(skill.canHandle(intent));
    }

    @Test
    void cannotHandle_emptyAppParam() {
        SystemCommand cmd = new SystemCommand("open-app", "", null);
        Intent intent = Intent.forTesting("open", "open",
                Optional.of(cmd), 1.0, Map.of("app", ""));
        assertFalse(skill.canHandle(intent));
    }

    // ── helper ──

    private Intent appIntent(String appName) {
        SystemCommand cmd = new SystemCommand("open-app", "", null);
        return Intent.forTesting("open " + appName, "open " + appName,
                Optional.of(cmd), 1.0, Map.of("app", appName));
    }
}
