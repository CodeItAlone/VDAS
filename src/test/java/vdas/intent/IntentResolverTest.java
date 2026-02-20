package vdas.intent;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import vdas.model.SystemCommand;

import java.util.List;
import java.util.Map;
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
                        List.of("sys info", "os info", "computer info")),
                new SystemCommand("open-app", "", null,
                        List.of("open app", "launch app", "start app", "run app")));
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
    void testResolve_openChrome_resolvesToOpenApp() {
        Intent result = resolver.resolve("open chrome");
        assertTrue(result.getResolvedCommand().isPresent());
        assertEquals("open-app", result.getResolvedCommand().get().getName());
        assertEquals("chrome", result.getParameters().get("app"));
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

    // ── Parameter extraction (open-app) ──

    @Test
    void testResolve_launchVscode_extractsAppParam() {
        Intent result = resolver.resolve("launch vscode");
        assertTrue(result.getResolvedCommand().isPresent());
        assertEquals("open-app", result.getResolvedCommand().get().getName());
        assertEquals("vscode", result.getParameters().get("app"));
    }

    @Test
    void testResolve_openAlone_noConfidentMatch() {
        Intent result = resolver.resolve("open");
        // "open" alone should not confidently match "open-app" (normalized: "open app")
        // It may fuzzy-match below threshold or be rejected
        if (result.getResolvedCommand().isPresent()) {
            // If it does match, it should NOT be "open-app" with high confidence
            assertNotEquals(1.0, result.getConfidence(),
                    "Bare 'open' must not be an exact/alias match to open-app");
        }
    }

    @Test
    void testResolve_openUnknownapp_resolvesButNoWhitelistMatch() {
        Intent result = resolver.resolve("open unknownapp");
        // May fuzzy-match to open-app, parameters should have app=unknownapp
        // The skill will reject it, not the resolver
        if (result.getResolvedCommand().isPresent()
                && "open-app".equals(result.getResolvedCommand().get().getName())) {
            assertEquals("unknownapp", result.getParameters().get("app"));
        }
    }

    @Test
    void testResolve_existingCommandsHaveEmptyParameters() {
        Intent result = resolver.resolve("list files");
        assertTrue(result.getResolvedCommand().isPresent());
        assertTrue(result.getParameters().isEmpty(),
                "Non-open-app commands should have empty parameters");
    }
}
