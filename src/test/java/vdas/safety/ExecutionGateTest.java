package vdas.safety;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import vdas.intent.DefaultAmbiguityDetector;
import vdas.intent.Intent;
import vdas.model.SystemCommand;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class ExecutionGateTest {

    private ExecutionGate gate;

    @BeforeEach
    void setUp() {
        gate = new ExecutionGate(new DefaultDangerClassifier(), new DefaultAmbiguityDetector());
    }

    // ── PRD validation matrix ──

    @Test
    void testHighConfidence_notDangerous_executes() {
        // "open chrome" — HIGH, not dangerous → EXECUTE
        SystemCommand cmd = new SystemCommand("open-app", "", null);
        Intent intent = Intent.forTesting("open chrome", "open chrome",
                Optional.of(cmd), 1.0);
        assertEquals(ExecutionGate.Decision.EXECUTE, gate.evaluate(intent));
    }

    @Test
    void testHighConfidence_dangerous_confirmsQuit() {
        // "quit" — HIGH, dangerous → CONFIRM
        SystemCommand cmd = new SystemCommand("quit", "", null);
        Intent intent = Intent.forTesting("quit", "quit",
                Optional.of(cmd), 1.0);
        assertEquals(ExecutionGate.Decision.CONFIRM, gate.evaluate(intent));
    }

    @Test
    void testHighConfidence_dangerous_confirmsShutdown() {
        SystemCommand cmd = new SystemCommand("shutdown", "", null);
        Intent intent = Intent.forTesting("shutdown", "shutdown",
                Optional.of(cmd), 1.0);
        assertEquals(ExecutionGate.Decision.CONFIRM, gate.evaluate(intent));
    }

    @Test
    void testHighConfidence_dangerous_confirmsDelete() {
        SystemCommand cmd = new SystemCommand("delete", "", null);
        Intent intent = Intent.forTesting("delete", "delete",
                Optional.of(cmd), 1.0);
        assertEquals(ExecutionGate.Decision.CONFIRM, gate.evaluate(intent));
    }

    @Test
    void testMediumConfidence_nonAmbiguous_confirms() {
        // Fuzzy match — MEDIUM, no candidates → CONFIRM
        SystemCommand cmd = new SystemCommand("list-files", "dir /b", null);
        Intent intent = Intent.forTesting("list filez", "list filez",
                Optional.of(cmd), 0.85);
        assertEquals(ExecutionGate.Decision.CONFIRM, gate.evaluate(intent));
    }

    @Test
    void testMediumConfidence_dangerous_confirms() {
        // "the quit" — MEDIUM, dangerous → CONFIRM (no candidates → not ambiguous)
        SystemCommand cmd = new SystemCommand("quit", "", null);
        Intent intent = Intent.forTesting("the quit", "quit",
                Optional.of(cmd), 0.80);
        assertEquals(ExecutionGate.Decision.CONFIRM, gate.evaluate(intent));
    }

    @Test
    void testLowConfidence_rejects() {
        // "random words" — LOW → REJECT
        Intent intent = Intent.forTesting("random words", "random words",
                Optional.empty(), 0.3);
        assertEquals(ExecutionGate.Decision.REJECT, gate.evaluate(intent));
    }

    // ── Unresolved intents rejected immediately ──

    @Test
    void testUnresolved_rejects_immediately() {
        Intent intent = Intent.forTesting("gibberish", "gibberish",
                Optional.empty(), 0.0);
        assertEquals(ExecutionGate.Decision.REJECT, gate.evaluate(intent));
    }

    @Test
    void testUnresolved_highScoreStillRejects() {
        Intent intent = Intent.forTesting("nothing", "nothing",
                Optional.empty(), 1.0);
        assertEquals(ExecutionGate.Decision.REJECT, gate.evaluate(intent));
    }

    // ── Non-dangerous HIGH commands ──

    @Test
    void testHighConfidence_listFiles_executes() {
        SystemCommand cmd = new SystemCommand("list-files", "dir /b", null);
        Intent intent = Intent.forTesting("list files", "list files",
                Optional.of(cmd), 1.0);
        assertEquals(ExecutionGate.Decision.EXECUTE, gate.evaluate(intent));
    }

    @Test
    void testHighConfidence_systemInfo_executes() {
        SystemCommand cmd = new SystemCommand("system-info", "systeminfo", null);
        Intent intent = Intent.forTesting("system info", "system info",
                Optional.of(cmd), 1.0);
        assertEquals(ExecutionGate.Decision.EXECUTE, gate.evaluate(intent));
    }

    // ── Phase 2.3: CLARIFY ──

    @Test
    void testMediumConfidence_ambiguous_clarifies() {
        // MEDIUM + 2 close-scoring candidates → CLARIFY
        SystemCommand cmd1 = new SystemCommand("system-info", "", null);
        SystemCommand cmd2 = new SystemCommand("java-version", "", null);
        Intent intent = Intent.forTesting("sys ver", "sys ver",
                Optional.of(cmd1), 0.85,
                List.of(cmd1, cmd2), List.of(0.85, 0.82));
        assertEquals(ExecutionGate.Decision.CLARIFY, gate.evaluate(intent));
    }

    @Test
    void testMediumConfidence_nonAmbiguous_singleCandidate_confirms() {
        // MEDIUM + 1 candidate → not ambiguous → CONFIRM
        SystemCommand cmd = new SystemCommand("list-files", "dir /b", null);
        Intent intent = Intent.forTesting("list filez", "list filez",
                Optional.of(cmd), 0.85,
                List.of(cmd), List.of(0.85));
        assertEquals(ExecutionGate.Decision.CONFIRM, gate.evaluate(intent));
    }
}
