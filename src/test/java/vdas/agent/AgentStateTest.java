package vdas.agent;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AgentStateTest {

    @Test
    void allStatesExist() {
        AgentState[] values = AgentState.values();
        assertEquals(4, values.length,
                "AgentState must have exactly 4 states");
    }

    @Test
    void defaultState_isIdle() {
        assertEquals(AgentState.IDLE, AgentState.values()[0],
                "First enum constant must be IDLE");
    }

    @Test
    void valueOf_roundTrips() {
        for (AgentState state : AgentState.values()) {
            assertEquals(state, AgentState.valueOf(state.name()),
                    "valueOf must round-trip for: " + state.name());
        }
    }
}
