package vdas.safety;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import vdas.intent.Intent;
import vdas.model.SystemCommand;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class ExecutionGateTest {

    private ExecutionGate gate;

    @BeforeEach
    void setUp() {
        gate = new ExecutionGate(new DefaultDangerClassifier());
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
    void testMediumConfidence_confirms() {
        // Fuzzy match — MEDIUM → CONFIRM regardless of danger
        SystemCommand cmd = new SystemCommand("list-files", "dir /b", null);
        Intent intent = Intent.forTesting("list filez", "list filez",
                Optional.of(cmd), 0.85);
        assertEquals(ExecutionGate.Decision.CONFIRM, gate.evaluate(intent));
    }

    @Test
    void testMediumConfidence_dangerous_confirms() {
        // "the quit" — MEDIUM, dangerous → CONFIRM
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

    // ── Refinement 2: Unresolved intents rejected immediately ──

    @Test
    void testUnresolved_rejects_immediately() {
        Intent intent = Intent.forTesting("gibberish", "gibberish",
                Optional.empty(), 0.0);
        assertEquals(ExecutionGate.Decision.REJECT, gate.evaluate(intent));
    }

    @Test
    void testUnresolved_highScoreStillRejects() {
        // Even if somehow confidence is high but no command → REJECT
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
}
