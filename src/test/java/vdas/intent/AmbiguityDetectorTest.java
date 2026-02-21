package vdas.intent;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import vdas.model.SystemCommand;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class AmbiguityDetectorTest {

    private AmbiguityDetector detector;

    @BeforeEach
    void setUp() {
        detector = new DefaultAmbiguityDetector();
    }

    @Test
    void testMediumConfidence_twoCandidates_closeScores_isAmbiguous() {
        SystemCommand cmd1 = new SystemCommand("system-info", "", null);
        SystemCommand cmd2 = new SystemCommand("java-version", "", null);
        Intent intent = Intent.forTesting("sys ver", "sys ver",
                Optional.of(cmd1), 0.85,
                List.of(cmd1, cmd2), List.of(0.85, 0.82));
        assertTrue(detector.isAmbiguous(intent));
    }

    @Test
    void testHighConfidence_notAmbiguous() {
        SystemCommand cmd1 = new SystemCommand("system-info", "", null);
        SystemCommand cmd2 = new SystemCommand("java-version", "", null);
        Intent intent = Intent.forTesting("system info", "system info",
                Optional.of(cmd1), 1.0,
                List.of(cmd1, cmd2), List.of(1.0, 0.85));
        assertFalse(detector.isAmbiguous(intent));
    }

    @Test
    void testLowConfidence_notAmbiguous() {
        SystemCommand cmd1 = new SystemCommand("system-info", "", null);
        SystemCommand cmd2 = new SystemCommand("java-version", "", null);
        Intent intent = Intent.forTesting("xyz", "xyz",
                Optional.of(cmd1), 0.3,
                List.of(cmd1, cmd2), List.of(0.3, 0.28));
        assertFalse(detector.isAmbiguous(intent));
    }

    @Test
    void testMediumConfidence_singleCandidate_notAmbiguous() {
        SystemCommand cmd = new SystemCommand("system-info", "", null);
        Intent intent = Intent.forTesting("system inf", "system inf",
                Optional.of(cmd), 0.85,
                List.of(cmd), List.of(0.85));
        assertFalse(detector.isAmbiguous(intent));
    }

    @Test
    void testMediumConfidence_noCandidates_notAmbiguous() {
        Intent intent = Intent.forTesting("fuzzy match", "fuzzy match",
                Optional.of(new SystemCommand("list-files", "", null)), 0.85,
                Collections.emptyList(), Collections.emptyList());
        assertFalse(detector.isAmbiguous(intent));
    }

    @Test
    void testMediumConfidence_twoCandidates_largeScoreGap_notAmbiguous() {
        SystemCommand cmd1 = new SystemCommand("system-info", "", null);
        SystemCommand cmd2 = new SystemCommand("java-version", "", null);
        // Gap = 0.15 > 0.10 → not ambiguous
        Intent intent = Intent.forTesting("sys info", "sys info",
                Optional.of(cmd1), 0.90,
                List.of(cmd1, cmd2), List.of(0.90, 0.75));
        assertFalse(detector.isAmbiguous(intent));
    }

    @Test
    void testMediumConfidence_twoCandidates_exactBoundary_isAmbiguous() {
        SystemCommand cmd1 = new SystemCommand("system-info", "", null);
        SystemCommand cmd2 = new SystemCommand("java-version", "", null);
        // Gap = exactly 0.10 → ambiguous (≤ 0.10)
        Intent intent = Intent.forTesting("sys ver", "sys ver",
                Optional.of(cmd1), 0.85,
                List.of(cmd1, cmd2), List.of(0.85, 0.75));
        assertTrue(detector.isAmbiguous(intent));
    }
}
