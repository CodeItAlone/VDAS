package vdas.session;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import vdas.intent.Intent;
import vdas.model.SystemCommand;
import vdas.skill.Skill;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class SessionContextTest {

    private SessionContext context;
    private Intent sampleIntent;
    private SystemCommand sampleCommand;
    private Skill sampleSkill;

    @BeforeEach
    void setUp() {
        context = new SessionContext();
        sampleCommand = new SystemCommand("system-info", "systeminfo", null);
        sampleIntent = Intent.forTesting("system info", "system-info",
                Optional.of(sampleCommand), 1.0);
        sampleSkill = new StubSkill();
    }

    @Test
    void initialState_isNull() {
        assertNull(context.getLastIntent());
        assertNull(context.getLastCommand());
        assertNull(context.getLastSkill());
        assertFalse(context.hasContext());
    }

    @Test
    void update_storesValues() {
        context.update(sampleIntent, sampleCommand, sampleSkill);

        assertEquals(sampleIntent, context.getLastIntent());
        assertEquals(sampleCommand, context.getLastCommand());
        assertEquals(sampleSkill, context.getLastSkill());
    }

    @Test
    void update_overwritesPrevious() {
        context.update(sampleIntent, sampleCommand, sampleSkill);

        // Create second set of values
        SystemCommand newCommand = new SystemCommand("list-files", "dir", null);
        Intent newIntent = Intent.forTesting("list files", "list-files",
                Optional.of(newCommand), 0.95);
        Skill newSkill = new StubSkill();

        context.update(newIntent, newCommand, newSkill);

        assertSame(newIntent, context.getLastIntent());
        assertSame(newCommand, context.getLastCommand());
        assertSame(newSkill, context.getLastSkill());

        // Verify old values are gone
        assertNotSame(sampleIntent, context.getLastIntent());
        assertNotSame(sampleCommand, context.getLastCommand());
        assertNotSame(sampleSkill, context.getLastSkill());
    }

    @Test
    void hasContext_returnsTrueAfterUpdate() {
        assertFalse(context.hasContext());
        context.update(sampleIntent, sampleCommand, sampleSkill);
        assertTrue(context.hasContext());
    }

    @Test
    void update_rejectsNullArguments() {
        assertThrows(NullPointerException.class,
                () -> context.update(null, sampleCommand, sampleSkill));
        assertThrows(NullPointerException.class,
                () -> context.update(sampleIntent, null, sampleSkill));
        assertThrows(NullPointerException.class,
                () -> context.update(sampleIntent, sampleCommand, null));

        // Context should remain empty after failed updates
        assertFalse(context.hasContext());
    }

    @Test
    void getters_returnSameReferences_notCopies() {
        context.update(sampleIntent, sampleCommand, sampleSkill);

        assertSame(sampleIntent, context.getLastIntent());
        assertSame(sampleCommand, context.getLastCommand());
        assertSame(sampleSkill, context.getLastSkill());
    }

    // ── Minimal stub for testing — no mocking framework needed ──

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
