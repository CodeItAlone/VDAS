package vdas.intent;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LevenshteinDistanceTest {

    @Test
    void testIdenticalStrings() {
        assertEquals(0, LevenshteinDistance.compute("hello", "hello"));
        assertEquals(1.0, LevenshteinDistance.similarity("hello", "hello"), 0.001);
    }

    @Test
    void testEmptyStrings() {
        assertEquals(0, LevenshteinDistance.compute("", ""));
        assertEquals(1.0, LevenshteinDistance.similarity("", ""), 0.001);
    }

    @Test
    void testOneEmpty() {
        assertEquals(5, LevenshteinDistance.compute("hello", ""));
        assertEquals(0.0, LevenshteinDistance.similarity("hello", ""), 0.001);
    }

    @Test
    void testSingleCharEdit() {
        // substitution
        assertEquals(1, LevenshteinDistance.compute("cat", "bat"));
        // insertion
        assertEquals(1, LevenshteinDistance.compute("cat", "cats"));
        // deletion
        assertEquals(1, LevenshteinDistance.compute("cats", "cat"));
    }

    @Test
    void testCompletelyDifferent() {
        assertEquals(3, LevenshteinDistance.compute("abc", "xyz"));
        assertEquals(0.0, LevenshteinDistance.similarity("abc", "xyz"), 0.001);
    }

    @Test
    void testSimilarityRange() {
        double sim = LevenshteinDistance.similarity("java version", "java virsion");
        assertTrue(sim >= 0.0 && sim <= 1.0, "Similarity should be in [0, 1]");
        assertTrue(sim > 0.75, "Similar strings should score above threshold");
    }

    @Test
    void testSymmetry() {
        assertEquals(
                LevenshteinDistance.compute("kitten", "sitting"),
                LevenshteinDistance.compute("sitting", "kitten"));
    }

    @Test
    void testNullInputThrows() {
        assertThrows(IllegalArgumentException.class, () -> LevenshteinDistance.compute(null, "a"));
        assertThrows(IllegalArgumentException.class, () -> LevenshteinDistance.similarity("a", null));
    }
}
