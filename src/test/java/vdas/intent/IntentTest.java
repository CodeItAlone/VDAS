package vdas.intent;

import org.junit.jupiter.api.Test;
import vdas.model.SystemCommand;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for Intent immutability and structure.
 */
class IntentTest {

    @Test
    void testImmutability_allFieldsMatchConstructorArgs() {
        SystemCommand cmd = new SystemCommand("test-cmd", "echo test", null);
        Intent intent = new Intent("test command", "test command", Optional.of(cmd), 0.95);

        assertEquals("test command", intent.getRawInput());
        assertEquals("test command", intent.getNormalizedInput());
        assertTrue(intent.getResolvedCommand().isPresent());
        assertEquals("test-cmd", intent.getResolvedCommand().get().getName());
        assertEquals(0.95, intent.getConfidence(), 0.001);
    }

    @Test
    void testImmutability_emptyResolvedCommand() {
        Intent intent = new Intent("unknown", "unknown", Optional.empty(), 0.0);

        assertEquals("unknown", intent.getRawInput());
        assertEquals("unknown", intent.getNormalizedInput());
        assertTrue(intent.getResolvedCommand().isEmpty());
        assertEquals(0.0, intent.getConfidence(), 0.001);
    }

    @Test
    void testConfidence_fullRange() {
        Intent zeroConfidence = new Intent("x", "x", Optional.empty(), 0.0);
        assertEquals(0.0, zeroConfidence.getConfidence(), 0.001);

        Intent fullConfidence = new Intent("x", "x", Optional.empty(), 1.0);
        assertEquals(1.0, fullConfidence.getConfidence(), 0.001);

        Intent midConfidence = new Intent("x", "x", Optional.empty(), 0.82);
        assertEquals(0.82, midConfidence.getConfidence(), 0.001);
    }

    @Test
    void testToString_containsAllFields() {
        SystemCommand cmd = new SystemCommand("list-files", "dir /b", null);
        Intent intent = new Intent("list files", "list files", Optional.of(cmd), 1.0);

        String str = intent.toString();
        assertTrue(str.contains("list files"), "toString should contain rawInput");
        assertTrue(str.contains("list-files"), "toString should contain resolved command name");
        assertTrue(str.contains("1.00"), "toString should contain formatted confidence");
    }

    @Test
    void testToString_noResolvedCommand() {
        Intent intent = new Intent("gibberish", "gibberish", Optional.empty(), 0.0);

        String str = intent.toString();
        assertTrue(str.contains("gibberish"), "toString should contain rawInput");
        assertTrue(str.contains("none"), "toString should show 'none' for empty resolved command");
    }

    @Test
    void testForTesting_factoryCreatesValidIntent() {
        SystemCommand cmd = new SystemCommand("test-cmd", "echo test", null);
        Intent intent = Intent.forTesting("raw", "normalized", Optional.of(cmd), 0.85);

        assertEquals("raw", intent.getRawInput());
        assertEquals("normalized", intent.getNormalizedInput());
        assertTrue(intent.getResolvedCommand().isPresent());
        assertEquals(0.85, intent.getConfidence(), 0.001);
    }

    @Test
    void testResolvedCommandAndConfidenceIndependent() {
        // Can have high confidence with no resolved command (e.g., rejected by
        // ambiguity)
        Intent highConfNoCmd = new Intent("ambig", "ambig", Optional.empty(), 0.90);
        assertTrue(highConfNoCmd.getResolvedCommand().isEmpty());
        assertEquals(0.90, highConfNoCmd.getConfidence(), 0.001);

        // Can have low confidence with a resolved command (edge case)
        SystemCommand cmd = new SystemCommand("test", "test", null);
        Intent lowConfWithCmd = new Intent("test", "test", Optional.of(cmd), 0.1);
        assertTrue(lowConfWithCmd.getResolvedCommand().isPresent());
        assertEquals(0.1, lowConfWithCmd.getConfidence(), 0.001);
    }
}
