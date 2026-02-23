package vdas.intent;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CommandSplitterTest {

    @Test
    void split_nullOrEmpty_returnsEmptyList() {
        assertTrue(CommandSplitter.split(null).isEmpty());
        assertTrue(CommandSplitter.split("").isEmpty());
        assertTrue(CommandSplitter.split("   ").isEmpty());
    }

    @Test
    void split_singleCommand_returnsOneStep() {
        List<String> steps = CommandSplitter.split("open chrome");
        assertEquals(1, steps.size());
        assertEquals("open chrome", steps.get(0));
    }

    @Test
    void split_withAnd_returnsTwoSteps() {
        List<String> steps = CommandSplitter.split("open chrome and open youtube");
        assertEquals(2, steps.size());
        assertEquals("open chrome", steps.get(0));
        assertEquals("open youtube", steps.get(1));
    }

    @Test
    void split_withThen_returnsTwoSteps() {
        List<String> steps = CommandSplitter.split("open chrome then close it");
        assertEquals(2, steps.size());
        assertEquals("open chrome", steps.get(0));
        assertEquals("close it", steps.get(1));
    }

    @Test
    void split_withAndThen_returnsTwoSteps() {
        List<String> steps = CommandSplitter.split("list files and then system info");
        assertEquals(2, steps.size());
        assertEquals("list files", steps.get(0));
        assertEquals("system info", steps.get(1));
    }

    @Test
    void split_multipleConnectors_returnsMultipleSteps() {
        List<String> steps = CommandSplitter.split("open chrome and open youtube then close it");
        assertEquals(3, steps.size());
        assertEquals("open chrome", steps.get(0));
        assertEquals("open youtube", steps.get(1));
        assertEquals("close it", steps.get(2));
    }

    @Test
    void split_handlesExtraWhitespace() {
        List<String> steps = CommandSplitter.split("  open chrome   and    close it  ");
        assertEquals(2, steps.size());
        assertEquals("open chrome", steps.get(0));
        assertEquals("close it", steps.get(1));
    }

    @Test
    void split_ignoresEmptySteps() {
        // "and and" -> middle empty step should be filtered
        List<String> steps = CommandSplitter.split("open chrome and and close it");
        assertEquals(2, steps.size());
        assertEquals("open chrome", steps.get(0));
        assertEquals("close it", steps.get(1));
    }

    @Test
    void split_requiresWordBoundaries() {
        // "android" contains "and", but shouldn't be split
        List<String> steps = CommandSplitter.split("open android studio");
        assertEquals(1, steps.size());
        assertEquals("open android studio", steps.get(0));

        // "athens" contains "then"
        steps = CommandSplitter.split("weather in athens");
        assertEquals(1, steps.size());
        assertEquals("weather in athens", steps.get(0));
    }
}
