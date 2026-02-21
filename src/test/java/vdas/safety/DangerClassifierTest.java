package vdas.safety;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import vdas.intent.Intent;
import vdas.model.SystemCommand;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class DangerClassifierTest {

    private DangerClassifier classifier;

    @BeforeEach
    void setUp() {
        classifier = new DefaultDangerClassifier();
    }

    // ── Dangerous commands ──

    @Test
    void testQuitIsDangerous() {
        Intent intent = intentWith("quit");
        assertTrue(classifier.isDangerous(intent));
    }

    @Test
    void testShutdownIsDangerous() {
        Intent intent = intentWith("shutdown");
        assertTrue(classifier.isDangerous(intent));
    }

    @Test
    void testRestartIsDangerous() {
        Intent intent = intentWith("restart");
        assertTrue(classifier.isDangerous(intent));
    }

    @Test
    void testDeleteIsDangerous() {
        Intent intent = intentWith("delete");
        assertTrue(classifier.isDangerous(intent));
    }

    @Test
    void testRemoveIsDangerous() {
        Intent intent = intentWith("remove");
        assertTrue(classifier.isDangerous(intent));
    }

    @Test
    void testFormatIsDangerous() {
        Intent intent = intentWith("format");
        assertTrue(classifier.isDangerous(intent));
    }

    // ── Non-dangerous commands ──

    @Test
    void testListFilesIsNotDangerous() {
        Intent intent = intentWith("list-files");
        assertFalse(classifier.isDangerous(intent));
    }

    @Test
    void testOpenAppIsNotDangerous() {
        Intent intent = intentWith("open-app");
        assertFalse(classifier.isDangerous(intent));
    }

    @Test
    void testSystemInfoIsNotDangerous() {
        Intent intent = intentWith("system-info");
        assertFalse(classifier.isDangerous(intent));
    }

    // ── Unresolved intent ──

    @Test
    void testUnresolvedIntentIsNotDangerous() {
        Intent intent = Intent.forTesting("random words", "random words",
                Optional.empty(), 0.3);
        assertFalse(classifier.isDangerous(intent));
    }

    // ── Helper ──

    private Intent intentWith(String commandName) {
        SystemCommand cmd = new SystemCommand(commandName, "", null);
        return Intent.forTesting(commandName, commandName,
                Optional.of(cmd), 1.0);
    }
}
