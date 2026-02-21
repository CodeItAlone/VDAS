package vdas.safety;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import vdas.intent.Intent;
import vdas.model.SystemCommand;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.Scanner;

import static org.junit.jupiter.api.Assertions.*;

class ClarificationPromptTest {

    private ClarificationPrompt prompt;
    private Intent ambiguousIntent;
    private SystemCommand cmd1;
    private SystemCommand cmd2;

    @BeforeEach
    void setUp() {
        prompt = new ClarificationPrompt();
        cmd1 = new SystemCommand("system-info", "systeminfo", null);
        cmd2 = new SystemCommand("java-version", "java -version", null);
        ambiguousIntent = Intent.forTesting("sys ver", "sys ver",
                Optional.of(cmd1), 0.85,
                List.of(cmd1, cmd2), List.of(0.85, 0.82));
    }

    @Test
    void testNumericSelection_first() {
        Optional<SystemCommand> result = prompt.ask(ambiguousIntent, scannerOf("1\n"));
        assertTrue(result.isPresent());
        assertEquals("system-info", result.get().getName());
    }

    @Test
    void testNumericSelection_second() {
        Optional<SystemCommand> result = prompt.ask(ambiguousIntent, scannerOf("2\n"));
        assertTrue(result.isPresent());
        assertEquals("java-version", result.get().getName());
    }

    @Test
    void testExactNameSelection() {
        Optional<SystemCommand> result = prompt.ask(ambiguousIntent, scannerOf("java-version\n"));
        assertTrue(result.isPresent());
        assertEquals("java-version", result.get().getName());
    }

    @Test
    void testExactNameSelection_caseInsensitive() {
        Optional<SystemCommand> result = prompt.ask(ambiguousIntent, scannerOf("System-Info\n"));
        assertTrue(result.isPresent());
        assertEquals("system-info", result.get().getName());
    }

    @Test
    void testGarbageInput_returnsEmpty() {
        Optional<SystemCommand> result = prompt.ask(ambiguousIntent, scannerOf("foo\n"));
        assertTrue(result.isEmpty());
    }

    @Test
    void testEmptyInput_returnsEmpty() {
        Optional<SystemCommand> result = prompt.ask(ambiguousIntent, scannerOf("\n"));
        assertTrue(result.isEmpty());
    }

    @Test
    void testOutOfRangeNumber_returnsEmpty() {
        Optional<SystemCommand> result = prompt.ask(ambiguousIntent, scannerOf("5\n"));
        assertTrue(result.isEmpty());
    }

    @Test
    void testZeroNumber_returnsEmpty() {
        Optional<SystemCommand> result = prompt.ask(ambiguousIntent, scannerOf("0\n"));
        assertTrue(result.isEmpty());
    }

    // ── Helper ──

    private Scanner scannerOf(String input) {
        return new Scanner(new ByteArrayInputStream(
                input.getBytes(StandardCharsets.UTF_8)));
    }
}
