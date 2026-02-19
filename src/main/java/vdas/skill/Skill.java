package vdas.skill;

import vdas.intent.Intent;

/**
 * Skill interface for the VDAS skill-based architecture.
 *
 * Each command domain implements this interface.
 * Skills must be stateless — no fields, no side-effects between calls.
 */
public interface Skill {

    /**
     * Returns true if this skill can handle the given intent.
     *
     * @param intent the intent to check
     * @return true if this skill should execute the intent
     */
    boolean canHandle(Intent intent);

    /**
     * Executes the given intent.
     * Only called after {@link #canHandle(Intent)} returns true.
     *
     * @param intent the intent to execute
     */
    void execute(Intent intent);
}
