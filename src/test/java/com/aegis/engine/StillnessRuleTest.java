package com.aegis.engine;

import com.aegis.dto.SensorEventDto;
import com.aegis.model.EventType;
import com.aegis.model.IncidentType;
import com.aegis.model.Posture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class StillnessRuleTest {

    private StillnessRule stillnessRule;

    @BeforeEach
    void setUp() {
        // 15 seconds demo threshold, sway threshold 0.02
        stillnessRule = new StillnessRule(15, 0.020);
    }

    @Test
    @DisplayName("True positive: Upright posture with sway < threshold for >= 15s triggers PROLONGED_STILLNESS")
    void testProlongedStillnessTriggers() {
        SensorEventDto event = new SensorEventDto();
        event.setType(EventType.POSE_STATE);
        event.setPosture(Posture.SITTING);
        event.setStillSeconds(16);
        event.setSwayScore(0.008);
        event.setRoom("living room");

        RuleDecision decision = stillnessRule.evaluate(event);
        assertTrue(decision.isTriggered(), "Prolonged low sway must trigger stillness check-in");
        assertEquals(IncidentType.PROLONGED_STILLNESS, decision.getIncidentType());
        assertTrue(decision.getCheckInPrompt().contains("are you okay"));
    }

    @Test
    @DisplayName("Active motion (sway > threshold) must NOT trigger stillness")
    void testActiveMotionDoesNotTrigger() {
        SensorEventDto event = new SensorEventDto();
        event.setType(EventType.POSE_STATE);
        event.setPosture(Posture.UPRIGHT);
        event.setStillSeconds(30);
        event.setSwayScore(0.085); // High motion sway

        RuleDecision decision = stillnessRule.evaluate(event);
        assertFalse(decision.isTriggered(), "Active sway motion must not trigger stillness alert");
    }

    @Test
    @DisplayName("Simulate Freeze shortcut triggers stillness verification")
    void testSimulateFreezeShortcut() {
        SensorEventDto event = new SensorEventDto();
        event.setType(EventType.SIMULATED_FREEZE);

        RuleDecision decision = stillnessRule.evaluate(event);
        assertTrue(decision.isTriggered());
        assertEquals(IncidentType.PROLONGED_STILLNESS, decision.getIncidentType());
    }
}
