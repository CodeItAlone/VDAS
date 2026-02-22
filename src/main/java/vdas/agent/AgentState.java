package vdas.agent;

/**
 * Tracks the current state of the VDAS agent in the interactive session.
 *
 * <p>
 * Prevents user responses (e.g. "yes", "no", numeric input) during
 * confirmation or clarification from being misinterpreted as new commands.
 * </p>
 *
 * <p>
 * <b>State transitions:</b>
 * </p>
 * 
 * <pre>
 *   IDLE ──→ AWAITING_CONFIRMATION  (gate returns CONFIRM)
 *   IDLE ──→ AWAITING_CLARIFICATION (gate returns CLARIFY)
 *   IDLE ──→ EXECUTING              (skill dispatch begins)
 *   AWAITING_* ──→ EXECUTING        (user confirms / clarifies)
 *   AWAITING_* ──→ IDLE             (user cancels / rejects)
 *   EXECUTING  ──→ IDLE             (execution completes)
 * </pre>
 *
 * <p>
 * Not thread-safe. Designed for single-user, single-threaded CLI runtime.
 * </p>
 * <p>
 * No concurrent states, no timeout logic, no persistence across restarts.
 * </p>
 */
public enum AgentState {

    /** Ready to accept a new command. */
    IDLE,

    /** Waiting for the user to confirm a dangerous or medium-confidence command. */
    AWAITING_CONFIRMATION,

    /** Waiting for the user to clarify an ambiguous command. */
    AWAITING_CLARIFICATION,

    /** A skill is currently executing. */
    EXECUTING
}
