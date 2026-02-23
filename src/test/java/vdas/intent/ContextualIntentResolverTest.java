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
        private SystemCommand listCmd;
        private Intent openChromeIntent;
        private Intent listFilesIntent;
        private Skill stubSkill;

        @BeforeEach
        void setUp() {
                resolver = new ContextualIntentResolver();
                context = new SessionContext();

                chromeCmd = new SystemCommand("open-app", "", null);
                listCmd = new SystemCommand("list-files", "dir /b", null);
                openChromeIntent = Intent.forTesting(
                                "open chrome", "open-chrome",
                                Optional.of(chromeCmd), 1.0,
                                Map.of("app", "chrome"));
                listFilesIntent = Intent.forTesting(
                                "list files", "list-files",
                                Optional.of(listCmd), 1.0);
                stubSkill = new StubSkill();
        }

        // ════════════════════════════════════════════════════════════
        // Strategy 0: Context-Aware Website Upgrade
        // ════════════════════════════════════════════════════════════

        @Test
        void websiteUpgrade_afterBrowser_returnsRewrittenIntent() {
                context.update(openChromeIntent, chromeCmd, stubSkill);

                // IntentResolver might resolve "open youtube" as a high/medium confidence app
                Intent openYoutubeIntent = Intent.forTesting(
                                "open youtube", "open-youtube", Optional.of(chromeCmd), 1.0, Map.of("app", "youtube"));

                Optional<Intent> result = resolver.resolve(openYoutubeIntent, context);

                assertTrue(result.isPresent(), "Should upgrade website target when browser is in context");
                assertEquals("chrome", result.get().getParameters().get("app"), "Should borrow browser from context");
                assertEquals("youtube", result.get().getParameters().get("url"),
                                "Should convert app=youtube to url=youtube");
        }

        @Test
        void websiteUpgrade_withoutBrowserContext_returnsEmpty() {
                // Context is empty
                Intent openYoutubeIntent = Intent.forTesting(
                                "open youtube", "open-youtube", Optional.of(chromeCmd), 1.0, Map.of("app", "youtube"));

                Optional<Intent> result = resolver.resolve(openYoutubeIntent, context);

                assertTrue(result.isEmpty(), "Should not upgrade if no browser in context");
        }

        @Test
        void websiteUpgrade_afterNonBrowserApp_returnsEmpty() {
                // Last app was 'notepad'
                Intent openNotepadIntent = Intent.forTesting(
                                "open notepad", "open-notepad", Optional.of(chromeCmd), 1.0, Map.of("app", "notepad"));
                context.update(openNotepadIntent, chromeCmd, stubSkill);

                Intent openYoutubeIntent = Intent.forTesting(
                                "open youtube", "open-youtube", Optional.of(chromeCmd), 1.0, Map.of("app", "youtube"));

                Optional<Intent> result = resolver.resolve(openYoutubeIntent, context);

                assertTrue(result.isEmpty(), "Should not upgrade if last app is not 'chrome'");
        }

        @Test
        void websiteUpgrade_nonWebsiteTarget_returnsEmpty() {
                context.update(openChromeIntent, chromeCmd, stubSkill);

                // "open notepad" -> notepad is not a whitelisted website
                Intent openNotepadIntent = Intent.forTesting(
                                "open notepad", "open-notepad", Optional.of(chromeCmd), 1.0, Map.of("app", "notepad"));

                Optional<Intent> result = resolver.resolve(openNotepadIntent, context);

                assertTrue(result.isEmpty(), "Should not upgrade if target is not a whitelisted website");
        }

        // ════════════════════════════════════════════════════════════
        // Strategy 1: Repeat phrases (existing tests, unchanged)
        // ════════════════════════════════════════════════════════════

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

                SystemCommand otherCmd = new SystemCommand("list-files", "dir /b", null);
                Intent highIntent = Intent.forTesting(
                                "again", "again", Optional.of(otherCmd), 1.0);

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

        // ════════════════════════════════════════════════════════════
        // Strategy 2: Close-it
        // ════════════════════════════════════════════════════════════

        @Test
        void closeIt_withAppContext_returnsCloseCommand() {
                context.update(openChromeIntent, chromeCmd, stubSkill);

                Intent closeIntent = Intent.forTesting(
                                "close it", "close-it", Optional.empty(), 0.0);
                Optional<Intent> result = resolver.resolve(closeIntent, context);

                assertTrue(result.isPresent(), "close-it should resolve when app context exists");
                assertTrue(result.get().getResolvedCommand().isPresent());
                assertEquals("chrome", result.get().getParameters().get("app"),
                                "Should carry the app name from context");
                assertEquals("close", result.get().getParameters().get("action"),
                                "Should set action to 'close'");
        }

        @Test
        void closeIt_withoutContext_returnsEmpty() {
                // No context.update() — context is empty
                Intent closeIntent = Intent.forTesting(
                                "close it", "close-it", Optional.empty(), 0.0);
                Optional<Intent> result = resolver.resolve(closeIntent, context);

                assertTrue(result.isEmpty(),
                                "close-it without context must be rejected");
        }

        @Test
        void closeIt_afterNonAppCommand_returnsEmpty() {
                // Last command was list-files, not open-app
                context.update(listFilesIntent, listCmd, stubSkill);

                Intent closeIntent = Intent.forTesting(
                                "close it", "close-it", Optional.empty(), 0.0);
                Optional<Intent> result = resolver.resolve(closeIntent, context);

                assertTrue(result.isEmpty(),
                                "close-it after non-app command must not fabricate");
        }

        @Test
        void closeVariants_allWork() {
                context.update(openChromeIntent, chromeCmd, stubSkill);

                String[] phrases = { "close", "close it", "close that" };

                for (String phrase : phrases) {
                        Intent intent = Intent.forTesting(phrase, phrase, Optional.empty(), 0.0);
                        Optional<Intent> result = resolver.resolve(intent, context);
                        assertTrue(result.isPresent(),
                                        "Close phrase should resolve: \"" + phrase + "\"");
                        assertEquals("chrome", result.get().getParameters().get("app"),
                                        "Should carry app name for: \"" + phrase + "\"");
                }
        }

        @Test
        void closeIt_doesNotMutateOriginalIntent() {
                context.update(openChromeIntent, chromeCmd, stubSkill);

                Intent originalIntent = Intent.forTesting(
                                "close it", "close-it", Optional.empty(), 0.0);
                Optional<SystemCommand> originalCmd = originalIntent.getResolvedCommand();

                Optional<Intent> result = resolver.resolve(originalIntent, context);

                assertTrue(result.isPresent());
                assertEquals(originalCmd, originalIntent.getResolvedCommand(),
                                "Original intent must not be mutated");
                assertNotSame(originalIntent, result.get());
        }

        // ════════════════════════════════════════════════════════════
        // Strategy 3: Contextual navigation
        // ════════════════════════════════════════════════════════════

        @Test
        void openTarget_afterBrowser_returnsNavigationIntent() {
                context.update(openChromeIntent, chromeCmd, stubSkill);

                // "open youtube" is unresolved (not a whitelisted app → confidence 0.0)
                Intent openYoutubeIntent = Intent.forTesting(
                                "open youtube", "open-youtube", Optional.empty(), 0.0);
                Optional<Intent> result = resolver.resolve(openYoutubeIntent, context);

                assertTrue(result.isPresent(),
                                "open <target> should resolve when app context exists");
                assertTrue(result.get().getResolvedCommand().isPresent());
                assertEquals("chrome", result.get().getParameters().get("app"),
                                "Should re-use last app from context");
                assertEquals("youtube", result.get().getParameters().get("url"),
                                "Should extract target as URL parameter");
        }

        @Test
        void openTarget_withoutContext_returnsEmpty() {
                // No context — "open youtube" should fail
                Intent openYoutubeIntent = Intent.forTesting(
                                "open youtube", "open-youtube", Optional.empty(), 0.0);
                Optional<Intent> result = resolver.resolve(openYoutubeIntent, context);

                assertTrue(result.isEmpty(),
                                "open <target> without context must be rejected");
        }

        @Test
        void openTarget_afterNonAppCommand_returnsEmpty() {
                // Last command was list-files, not open-app
                context.update(listFilesIntent, listCmd, stubSkill);

                Intent openYoutubeIntent = Intent.forTesting(
                                "open youtube", "open-youtube", Optional.empty(), 0.0);
                Optional<Intent> result = resolver.resolve(openYoutubeIntent, context);

                assertTrue(result.isEmpty(),
                                "open <target> after non-app command must not fabricate");
        }

        @Test
        void openTarget_highConfidence_neverOverridden() {
                context.update(openChromeIntent, chromeCmd, stubSkill);

                // HIGH confidence intent (e.g., "open notepad" matched a real app)
                SystemCommand notepadCmd = new SystemCommand("open-app", "", null);
                Intent highIntent = Intent.forTesting(
                                "open notepad", "open-notepad",
                                Optional.of(notepadCmd), 1.0, Map.of("app", "notepad"));

                Optional<Intent> result = resolver.resolve(highIntent, context);
                assertTrue(result.isEmpty(),
                                "HIGH confidence must never be overridden, even for navigation");
        }

        @Test
        void openTarget_verbVariants_work() {
                context.update(openChromeIntent, chromeCmd, stubSkill);

                // Test "go to gmail"
                Intent goToIntent = Intent.forTesting(
                                "go to gmail", "go-to-gmail", Optional.empty(), 0.0);
                Optional<Intent> result = resolver.resolve(goToIntent, context);

                assertTrue(result.isPresent(),
                                "'go to <target>' should resolve when app context exists");
                assertEquals("gmail", result.get().getParameters().get("url"));
        }

        @Test
        void openTarget_bareVerb_returnsEmpty() {
                context.update(openChromeIntent, chromeCmd, stubSkill);

                // Just "open" with no target
                Intent bareOpenIntent = Intent.forTesting(
                                "open", "open", Optional.empty(), 0.0);
                Optional<Intent> result = resolver.resolve(bareOpenIntent, context);

                assertTrue(result.isEmpty(),
                                "Bare verb without target must not fabricate");
        }

        @Test
        void navigationDoesNotMutateOriginalIntent() {
                context.update(openChromeIntent, chromeCmd, stubSkill);

                Intent originalIntent = Intent.forTesting(
                                "open youtube", "open-youtube", Optional.empty(), 0.0);
                Optional<SystemCommand> originalCmd = originalIntent.getResolvedCommand();

                Optional<Intent> result = resolver.resolve(originalIntent, context);

                assertTrue(result.isPresent());
                assertEquals(originalCmd, originalIntent.getResolvedCommand(),
                                "Original intent must not be mutated");
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
