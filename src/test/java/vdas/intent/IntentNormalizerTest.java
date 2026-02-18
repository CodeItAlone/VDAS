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

    // ── Leading stop-word stripping (ASR article fix) ──

    @Test
    void testLeadingThe_stripped() {
        assertEquals("quit", IntentNormalizer.normalize("the quit"));
        assertEquals("system info", IntentNormalizer.normalize("the system info"));
        assertEquals("list files", IntentNormalizer.normalize("the list files"));
    }

    @Test
    void testLeadingA_stripped() {
        assertEquals("quit", IntentNormalizer.normalize("a quit"));
    }

    @Test
    void testLeadingAn_stripped() {
        assertEquals("system info", IntentNormalizer.normalize("an system info"));
    }

    @Test
    void testMiddleArticle_preserved() {
        assertEquals("list the files", IntentNormalizer.normalize("list the files"));
        assertEquals("run a command", IntentNormalizer.normalize("run a command"));
    }

    @Test
    void testBareArticle_returnsEmpty() {
        assertEquals("", IntentNormalizer.normalize("the"));
        assertEquals("", IntentNormalizer.normalize("a"));
        assertEquals("", IntentNormalizer.normalize("an"));
    }

    @Test
    void testLeadingArticle_withVoiceInputMatching() {
        // "the system info" (ASR output) should normalize to match "system-info"
        // (command name)
        String voiceInput = IntentNormalizer.normalize("the system info");
        String cmdName = IntentNormalizer.normalizeCommandName("system-info");
        assertEquals(voiceInput, cmdName);
    }
}
