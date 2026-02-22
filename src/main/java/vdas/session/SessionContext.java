package vdas.session;

import vdas.intent.Intent;
import vdas.model.SystemCommand;
import vdas.skill.Skill;

import java.util.Objects;

/**
 * Short-lived, session-scoped context that stores the last successfully
 * executed action during a single JVM runtime.
 *
 * <p>
 * <b>Scope:</b> In-memory only — no persistence, no serialization.
 * All state is lost when the JVM exits.
 * </p>
 *
 * <p>
 * <b>Thread safety:</b> This class is <em>NOT</em> thread-safe.
 * It is designed for a single-user, single-threaded CLI runtime.
 * </p>
 *
 * <p>
 * <b>Update contract:</b> Call {@link #update} only after a skill has
 * been resolved, executed successfully, and returned without throwing.
 * Never update context before execution completes.
 * </p>
 *
 * <p>
 * <b>Phase 3.1 — storage only.</b> This class stores data for later
 * phases. It does not influence decision-making or command resolution.
 * </p>
 */
public class SessionContext {

    private Intent lastIntent;
    private SystemCommand lastCommand;
    private Skill lastSkill;

    /**
     * Updates the session context after a successful skill execution.
     *
     * <p>
     * Overwrites any previously stored values. Stores the exact
     * object references passed in — no defensive copying.
     * </p>
     *
     * @param intent  the intent that was executed (must not be null)
     * @param command the resolved system command (must not be null)
     * @param skill   the skill that executed the intent (must not be null)
     * @throws NullPointerException if any argument is null
     */
    public void update(Intent intent, SystemCommand command, Skill skill) {
        Objects.requireNonNull(intent, "intent must not be null");
        Objects.requireNonNull(command, "command must not be null");
        Objects.requireNonNull(skill, "skill must not be null");

        // TODO: Validate logical consistency (e.g., skill.canHandle(intent))
        // once enforcement is feasible in a later phase.

        this.lastIntent = intent;
        this.lastCommand = command;
        this.lastSkill = skill;
    }

    /**
     * Returns the last successfully executed intent, or {@code null}
     * if no execution has occurred yet.
     */
    public Intent getLastIntent() {
        return lastIntent;
    }

    /**
     * Returns the last successfully resolved command, or {@code null}
     * if no execution has occurred yet.
     */
    public SystemCommand getLastCommand() {
        return lastCommand;
    }

    /**
     * Returns the last skill that executed successfully, or {@code null}
     * if no execution has occurred yet.
     */
    public Skill getLastSkill() {
        return lastSkill;
    }

    /**
     * Returns {@code true} only if all three context fields (intent,
     * command, skill) are non-null — i.e., at least one successful
     * execution has occurred.
     */
    public boolean hasContext() {
        return lastIntent != null && lastCommand != null && lastSkill != null;
    }

    /**
     * Explicitly resets all context fields to {@code null}.
     *
     * <p>
     * Not required in Phase 3.1, but provided for future phases
     * that may need to clear session state (e.g., mode switches,
     * explicit user resets).
     * </p>
     */
    public void clear() {
        this.lastIntent = null;
        this.lastCommand = null;
        this.lastSkill = null;
    }
}
