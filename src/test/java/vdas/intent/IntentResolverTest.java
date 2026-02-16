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
        Optional<SystemCommand> result = resolver.resolve("list files");
        assertTrue(result.isPresent());
        assertEquals("list-files", result.get().getName());
    }

    @Test
    void testExactMatch_javaVersion() {
        Optional<SystemCommand> result = resolver.resolve("java version");
        assertTrue(result.isPresent());
        assertEquals("java-version", result.get().getName());
    }

    @Test
    void testExactMatch_caseInsensitive() {
        Optional<SystemCommand> result = resolver.resolve("JAVA VERSION");
        assertTrue(result.isPresent());
        assertEquals("java-version", result.get().getName());
    }

    // ── Alias matches ──

    @Test
    void testAliasMatch_showFiles() {
        Optional<SystemCommand> result = resolver.resolve("show files");
        assertTrue(result.isPresent());
        assertEquals("list-files", result.get().getName());
    }

    @Test
    void testAliasMatch_checkJava() {
        Optional<SystemCommand> result = resolver.resolve("check java");
        assertTrue(result.isPresent());
        assertEquals("java-version", result.get().getName());
    }

    @Test
    void testAliasMatch_sysInfo() {
        Optional<SystemCommand> result = resolver.resolve("sys info");
        assertTrue(result.isPresent());
        assertEquals("system-info", result.get().getName());
    }

    // ── Fuzzy matches ──

    @Test
    void testFuzzyMatch_javaVirsion() {
        Optional<SystemCommand> result = resolver.resolve("java virsion");
        assertTrue(result.isPresent());
        assertEquals("java-version", result.get().getName());
    }

    // ── Safe rejections ──

    @Test
    void testReject_deleteFiles() {
        Optional<SystemCommand> result = resolver.resolve("delete files");
        assertTrue(result.isEmpty(), "Unknown command 'delete files' must be rejected");
    }

    @Test
    void testReject_openChrome() {
        Optional<SystemCommand> result = resolver.resolve("open chrome");
        assertTrue(result.isEmpty(), "Unknown command 'open chrome' must be rejected");
    }

    @Test
    void testReject_formatDisk() {
        Optional<SystemCommand> result = resolver.resolve("format disk");
        assertTrue(result.isEmpty(), "Unknown command 'format disk' must be rejected");
    }

    @Test
    void testReject_nullInput() {
        Optional<SystemCommand> result = resolver.resolve(null);
        assertTrue(result.isEmpty());
    }

    @Test
    void testReject_emptyInput() {
        Optional<SystemCommand> result = resolver.resolve("");
        assertTrue(result.isEmpty());
    }

    @Test
    void testReject_blankInput() {
        Optional<SystemCommand> result = resolver.resolve("   ");
        assertTrue(result.isEmpty());
    }
}
