package vdas.intent;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import vdas.model.SystemCommand;
import vdas.session.SessionContext;
import vdas.skill.Skill;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class ContextualIntentResolverTest {

    private ContextualIntentResolver resolver;
    private SessionContext context;

    // ── Shared fixtures ──
    private SystemCommand chromeCmd;
    private Intent openChromeIntent;
    private Skill stubSkill;

    @BeforeEach
    void setUp() {
        resolver = new ContextualIntentResolver();
        context = new SessionContext();

        chromeCmd = new SystemCommand("open-app", "", null);
        openChromeIntent = Intent.forTesting(
                "open chrome", "open-chrome",
                Optional.of(chromeCmd), 1.0,
                Map.of("app", "chrome"));
        stubSkill = new StubSkill();
    }

    @Test
    void repeatPhrase_withContext_returnsLastCommand() {
        context.update(openChromeIntent, chromeCmd, stubSkill);

        Intent againIntent = Intent.forTesting("again", "again", Optional.empty(), 0.0);
        Optional<Intent> result = resolver.resolve(againIntent, context);

        assertTrue(result.isPresent());
        assertTrue(result.get().getResolvedCommand().isPresent());
        assertSame(chromeCmd, result.get().getResolvedCommand().get());
    }

    @Test
    void repeatPhrase_withoutContext_returnsEmpty() {
        // No context.update() — context is empty
        Intent againIntent = Intent.forTesting("again", "again", Optional.empty(), 0.0);
        Optional<Intent> result = resolver.resolve(againIntent, context);

        assertTrue(result.isEmpty());
    }

    @Test
    void highConfidence_neverOverridden() {
        context.update(openChromeIntent, chromeCmd, stubSkill);

        SystemCommand listCmd = new SystemCommand("list-files", "dir /b", null);
        Intent highIntent = Intent.forTesting(
                "again", "again", Optional.of(listCmd), 1.0);

        Optional<Intent> result = resolver.resolve(highIntent, context);
        assertTrue(result.isEmpty(), "HIGH confidence must never be overridden");
    }

    @Test
    void nonRepeatLowIntent_returnsEmpty() {
        context.update(openChromeIntent, chromeCmd, stubSkill);

        Intent randomIntent = Intent.forTesting(
                "hello world", "hello-world", Optional.empty(), 0.0);
        Optional<Intent> result = resolver.resolve(randomIntent, context);

        assertTrue(result.isEmpty(),
                "Non-repeat phrases must not trigger contextual resolution");
    }

    @Test
    void repeatPreservesParameters() {
        context.update(openChromeIntent, chromeCmd, stubSkill);

        Intent againIntent = Intent.forTesting("again", "again", Optional.empty(), 0.0);
        Optional<Intent> result = resolver.resolve(againIntent, context);

        assertTrue(result.isPresent());
        assertEquals("chrome", result.get().getParameters().get("app"),
                "Repeat must preserve parameters from last intent");
    }

    @Test
    void mediumConfidence_alreadyResolved_returnsEmpty() {
        context.update(openChromeIntent, chromeCmd, stubSkill);

        SystemCommand sysInfoCmd = new SystemCommand("system-info", "systeminfo", null);
        Intent mediumResolved = Intent.forTesting(
                "again", "again", Optional.of(sysInfoCmd), 0.85);

        Optional<Intent> result = resolver.resolve(mediumResolved, context);
        assertTrue(result.isEmpty(),
                "Already-resolved MEDIUM+ intents must not be overridden");
    }

    @Test
    void variousRepeatPhrases_allWork() {
        context.update(openChromeIntent, chromeCmd, stubSkill);

        String[] phrases = {
                "again", "repeat", "do it again", "do that again",
                "same", "one more time", "repeat that", "run it again"
        };

        for (String phrase : phrases) {
            Intent intent = Intent.forTesting(phrase, phrase, Optional.empty(), 0.0);
            Optional<Intent> result = resolver.resolve(intent, context);
            assertTrue(result.isPresent(),
                    "Repeat phrase should resolve: \"" + phrase + "\"");
            assertSame(chromeCmd, result.get().getResolvedCommand().get(),
                    "Should return lastCommand for: \"" + phrase + "\"");
        }
    }

    @Test
    void repeatDoesNotMutateOriginalIntent() {
        context.update(openChromeIntent, chromeCmd, stubSkill);

        Intent originalIntent = Intent.forTesting(
                "again", "again", Optional.empty(), 0.0);

        // Capture original state
        String originalRaw = originalIntent.getRawInput();
        Optional<SystemCommand> originalCmd = originalIntent.getResolvedCommand();
        double originalConfidence = originalIntent.getConfidence();

        // Perform contextual resolution
        Optional<Intent> result = resolver.resolve(originalIntent, context);

        assertTrue(result.isPresent());

        // Verify original intent is NOT mutated
        assertEquals(originalRaw, originalIntent.getRawInput());
        assertEquals(originalCmd, originalIntent.getResolvedCommand());
        assertEquals(originalConfidence, originalIntent.getConfidence());

        // Verify result is a DIFFERENT object
        assertNotSame(originalIntent, result.get());
    }

    // ── Minimal stub ──
    private static class StubSkill implements Skill {
        @Override
        public boolean canHandle(Intent intent) {
            return true;
        }

        @Override
        public void execute(Intent intent) {
            // no-op
        }
    }
}
