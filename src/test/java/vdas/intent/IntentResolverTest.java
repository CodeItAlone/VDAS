package vdas.intent;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import vdas.model.SystemCommand;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class IntentResolverTest {

    private IntentResolver resolver;

    @BeforeEach
    void setUp() {
        List<SystemCommand> commands = List.of(
                new SystemCommand("list-files", "dir /b", null,
                        List.of("show files", "display files", "dir", "ls")),
                new SystemCommand("java-version", "java -version", null,
                        List.of("check java", "java ver", "jdk version", "java info")),
                new SystemCommand("system-info", "systeminfo", null,
                        List.of("sys info", "os info", "computer info")));
        resolver = new IntentResolver(commands);
    }

    // ── Exact matches ──

    @Test
    void testExactMatch_listFiles() {
        Intent result = resolver.resolve("list files");
        assertTrue(result.getResolvedCommand().isPresent());
        assertEquals("list-files", result.getResolvedCommand().get().getName());
        assertEquals(1.0, result.getConfidence());
    }

    @Test
    void testExactMatch_javaVersion() {
        Intent result = resolver.resolve("java version");
        assertTrue(result.getResolvedCommand().isPresent());
        assertEquals("java-version", result.getResolvedCommand().get().getName());
        assertEquals(1.0, result.getConfidence());
    }

    @Test
    void testExactMatch_caseInsensitive() {
        Intent result = resolver.resolve("JAVA VERSION");
        assertTrue(result.getResolvedCommand().isPresent());
        assertEquals("java-version", result.getResolvedCommand().get().getName());
        assertEquals(1.0, result.getConfidence());
    }

    // ── Alias matches ──

    @Test
    void testAliasMatch_showFiles() {
        Intent result = resolver.resolve("show files");
        assertTrue(result.getResolvedCommand().isPresent());
        assertEquals("list-files", result.getResolvedCommand().get().getName());
        assertEquals(1.0, result.getConfidence());
    }

    @Test
    void testAliasMatch_checkJava() {
        Intent result = resolver.resolve("check java");
        assertTrue(result.getResolvedCommand().isPresent());
        assertEquals("java-version", result.getResolvedCommand().get().getName());
        assertEquals(1.0, result.getConfidence());
    }

    @Test
    void testAliasMatch_sysInfo() {
        Intent result = resolver.resolve("sys info");
        assertTrue(result.getResolvedCommand().isPresent());
        assertEquals("system-info", result.getResolvedCommand().get().getName());
        assertEquals(1.0, result.getConfidence());
    }

    // ── Fuzzy matches ──

    @Test
    void testFuzzyMatch_javaVirsion() {
        Intent result = resolver.resolve("java virsion");
        assertTrue(result.getResolvedCommand().isPresent());
        assertEquals("java-version", result.getResolvedCommand().get().getName());
        assertTrue(result.getConfidence() >= 0.75, "Fuzzy confidence should be >= 0.75");
        assertTrue(result.getConfidence() < 1.0, "Fuzzy confidence should be < 1.0");
    }

    // ── Safe rejections ──

    @Test
    void testReject_deleteFiles() {
        Intent result = resolver.resolve("delete files");
        assertTrue(result.getResolvedCommand().isEmpty(), "Unknown command 'delete files' must be rejected");
        assertTrue(result.getConfidence() < 0.75);
    }

    @Test
    void testReject_openChrome() {
        Intent result = resolver.resolve("open chrome");
        assertTrue(result.getResolvedCommand().isEmpty(), "Unknown command 'open chrome' must be rejected");
    }

    @Test
    void testReject_formatDisk() {
        Intent result = resolver.resolve("format disk");
        assertTrue(result.getResolvedCommand().isEmpty(), "Unknown command 'format disk' must be rejected");
    }

    @Test
    void testReject_nullInput() {
        Intent result = resolver.resolve(null);
        assertTrue(result.getResolvedCommand().isEmpty());
        assertEquals(0.0, result.getConfidence());
    }

    @Test
    void testReject_emptyInput() {
        Intent result = resolver.resolve("");
        assertTrue(result.getResolvedCommand().isEmpty());
        assertEquals(0.0, result.getConfidence());
    }

    @Test
    void testReject_blankInput() {
        Intent result = resolver.resolve("   ");
        assertTrue(result.getResolvedCommand().isEmpty());
        assertEquals(0.0, result.getConfidence());
    }

    // ── resolveByCommand ──

    @Test
    void testResolveByCommand_returnsFullConfidence() {
        SystemCommand cmd = new SystemCommand("list-files", "dir /b", null);
        Intent result = resolver.resolveByCommand("1", cmd);

        assertTrue(result.getResolvedCommand().isPresent());
        assertEquals("list-files", result.getResolvedCommand().get().getName());
        assertEquals(1.0, result.getConfidence());
        assertEquals("1", result.getRawInput());
    }

    // ── Intent structure ──

    @Test
    void testResolve_preservesRawInput() {
        Intent result = resolver.resolve("LIST FILES");
        assertEquals("LIST FILES", result.getRawInput());
        assertEquals("list files", result.getNormalizedInput());
    }
}
