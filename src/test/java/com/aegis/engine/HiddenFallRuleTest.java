package com.aegis.engine;

import com.aegis.dto.SensorEventDto;
import com.aegis.model.EventType;
import com.aegis.model.IncidentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class HiddenFallRuleTest {

    private HiddenFallRule hiddenFallRule;

    @BeforeEach
    void setUp() {
        hiddenFallRule = new HiddenFallRule(10); // 10 second window
    }

    @Test
    @DisplayName("True positive: Impact sound followed by person lost within 5s triggers HIDDEN_FALL_SUSPECTED")
    void testImpactFollowedByDisappearanceTriggers() {
        Instant now = Instant.now();

        // 1. Loud impact sound in bathroom
        SensorEventDto impact = new SensorEventDto();
        impact.setType(EventType.IMPACT_SOUND);
        impact.setSoundLevel(0.95);
        impact.setRoom("bathroom");
        impact.setTimestamp(now);
        assertFalse(hiddenFallRule.evaluate(impact).isTriggered(), "Impact alone should not trigger alert yet");

        // 2. Person lost from view 4 seconds later
        SensorEventDto personLost = new SensorEventDto();
        personLost.setType(EventType.PERSON_LOST);
        personLost.setRoom("bathroom");
        personLost.setTimestamp(now.plusSeconds(4));

        RuleDecision decision = hiddenFallRule.evaluate(personLost);
        assertTrue(decision.isTriggered(), "Impact + disappearance within window must trigger hidden fall");
        assertEquals(IncidentType.HIDDEN_FALL_SUSPECTED, decision.getIncidentType());
        assertTrue(decision.getSummary().contains("bathroom"));
    }

    @Test
    @DisplayName("Disappearance long after impact (>10s window) must NOT trigger")
    void testExpiredImpactWindowDoesNotTrigger() {
        Instant now = Instant.now();

        // 1. Loud impact
        SensorEventDto impact = new SensorEventDto();
        impact.setType(EventType.IMPACT_SOUND);
        impact.setTimestamp(now);
        hiddenFallRule.evaluate(impact);

        // 2. Disappearance 20 seconds later
        SensorEventDto personLost = new SensorEventDto();
        personLost.setType(EventType.PERSON_LOST);
        personLost.setTimestamp(now.plusSeconds(20));

        RuleDecision decision = hiddenFallRule.evaluate(personLost);
        assertFalse(decision.isTriggered(), "Disappearance after window expiration must not trigger");
    }

    @Test
    @DisplayName("Simulate Hidden Fall shortcut button triggers check-in")
    void testSimulatedFallShortcut() {
        SensorEventDto simEvent = new SensorEventDto();
        simEvent.setType(EventType.SIMULATED_FALL);
        simEvent.setRoom("hallway");

        RuleDecision decision = hiddenFallRule.evaluate(simEvent);
        assertTrue(decision.isTriggered());
        assertEquals(IncidentType.HIDDEN_FALL_SUSPECTED, decision.getIncidentType());
    }
}
