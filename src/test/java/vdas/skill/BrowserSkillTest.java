package vdas.skill;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import vdas.intent.Intent;
import vdas.model.SystemCommand;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BrowserSkillTest {

    private BrowserSkill skill;
    private SystemCommand openAppCommand;

    @BeforeEach
    void setUp() {
        skill = new BrowserSkill();
        openAppCommand = new SystemCommand("open-app", "desc", "dir");
    }

    @Test
    void canHandle_validBrowserAndWebsite_returnsTrue() {
        Intent intent = Intent.forTesting(
                "open youtube", "open youtube", Optional.of(openAppCommand), 1.0,
                Map.of("app", "chrome", "url", "youtube"));

        assertTrue(skill.canHandle(intent), "Should handle valid whitelisted browser and website");
    }

    @Test
    void canHandle_missingUrl_returnsFalse() {
        Intent intent = Intent.forTesting(
                "open chrome", "open chrome", Optional.of(openAppCommand), 1.0,
                Map.of("app", "chrome"));

        assertFalse(skill.canHandle(intent), "Should reject if url parameter is missing");
    }

    @Test
    void canHandle_missingApp_returnsFalse() {
        Intent intent = Intent.forTesting(
                "open youtube", "open youtube", Optional.of(openAppCommand), 1.0,
                Map.of("url", "youtube"));

        assertFalse(skill.canHandle(intent), "Should reject if app parameter is missing");
    }

    @Test
    void canHandle_nonWhitelistedWebsite_returnsFalse() {
        Intent intent = Intent.forTesting(
                "open facebook", "open facebook", Optional.of(openAppCommand), 1.0,
                Map.of("app", "chrome", "url", "facebook"));

        assertFalse(skill.canHandle(intent), "Should reject non-whitelisted websites");
    }

    @Test
    void canHandle_nonWhitelistedBrowser_returnsFalse() {
        Intent intent = Intent.forTesting(
                "open youtube", "open youtube", Optional.of(openAppCommand), 1.0,
                Map.of("app", "firefox", "url", "youtube"));

        assertFalse(skill.canHandle(intent), "Should reject non-whitelisted browsers");
    }

    @Test
    void canHandle_wrongCommand_returnsFalse() {
        SystemCommand otherCommand = new SystemCommand("system-info", "desc", "dir");
        Intent intent = Intent.forTesting(
                "system info", "system info", Optional.of(otherCommand), 1.0,
                Map.of("app", "chrome", "url", "youtube"));

        assertFalse(skill.canHandle(intent), "Should only handle 'open-app' command");
    }
}
