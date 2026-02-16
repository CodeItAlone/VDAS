package vdas.intent;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class IntentNormalizerTest {

    @Test
    void testNullInput() {
        assertEquals("", IntentNormalizer.normalize(null));
        assertEquals("", IntentNormalizer.normalizeCommandName(null));
    }

    @Test
    void testEmptyAndBlankInput() {
        assertEquals("", IntentNormalizer.normalize(""));
        assertEquals("", IntentNormalizer.normalize("   "));
    }

    @Test
    void testLowercasing() {
        assertEquals("hello world", IntentNormalizer.normalize("Hello World"));
        assertEquals("java version", IntentNormalizer.normalize("JAVA VERSION"));
    }

    @Test
    void testPunctuationRemoval() {
        assertEquals("list files", IntentNormalizer.normalize("list files!"));
        assertEquals("hello world", IntentNormalizer.normalize("hello, world."));
    }

    @Test
    void testSpaceNormalization() {
        assertEquals("list files", IntentNormalizer.normalize("  list   files  "));
    }

    @Test
    void testHyphenConversionInCommandNames() {
        assertEquals("list files", IntentNormalizer.normalizeCommandName("list-files"));
        assertEquals("java version", IntentNormalizer.normalizeCommandName("java-version"));
        assertEquals("system info", IntentNormalizer.normalizeCommandName("system-info"));
    }

    @Test
    void testUnderscoreConversionInCommandNames() {
        assertEquals("list files", IntentNormalizer.normalizeCommandName("list_files"));
    }

    @Test
    void testVoiceInputMatchesCommandName() {
        String voiceInput = IntentNormalizer.normalize("list files");
        String cmdName = IntentNormalizer.normalizeCommandName("list-files");
        assertEquals(voiceInput, cmdName);
    }
}
