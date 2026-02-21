package vdas.safety;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import vdas.intent.Intent;
import vdas.model.SystemCommand;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.Scanner;

import static org.junit.jupiter.api.Assertions.*;

class ConfirmationManagerTest {

    private ConfirmationManager mgr;
    private Intent testIntent;

    @BeforeEach
    void setUp() {
        mgr = new ConfirmationManager();
        SystemCommand cmd = new SystemCommand("quit", "", null);
        testIntent = Intent.forTesting("quit", "quit",
                Optional.of(cmd), 1.0);
    }

    // ── Accepted confirmations ──

    @Test
    void testYesConfirms() {
        assertTrue(mgr.confirm(testIntent, scannerOf("yes\n")));
    }

    @Test
    void testYeahConfirms() {
        assertTrue(mgr.confirm(testIntent, scannerOf("yeah\n")));
    }

    @Test
    void testConfirmConfirms() {
        assertTrue(mgr.confirm(testIntent, scannerOf("confirm\n")));
    }

    @Test
    void testYesCaseInsensitive() {
        assertTrue(mgr.confirm(testIntent, scannerOf("YES\n")));
    }

    // ── Rejections ──

    @Test
    void testNoRejects() {
        assertFalse(mgr.confirm(testIntent, scannerOf("no\n")));
    }

    @Test
    void testCancelRejects() {
        assertFalse(mgr.confirm(testIntent, scannerOf("cancel\n")));
    }

    @Test
    void testGarbageRejects() {
        assertFalse(mgr.confirm(testIntent, scannerOf("asdf\n")));
    }

    @Test
    void testEmptyInputRejects() {
        assertFalse(mgr.confirm(testIntent, scannerOf("\n")));
    }

    // ── Helper ──

    private Scanner scannerOf(String input) {
        return new Scanner(new ByteArrayInputStream(
                input.getBytes(StandardCharsets.UTF_8)));
    }
}
