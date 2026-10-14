package com.aegis.engine;

import com.aegis.dto.SensorEventDto;
import com.aegis.model.EventType;
import com.aegis.model.IncidentType;
import com.aegis.model.Posture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class FallRuleTest {

    private FallRule fallRule;

    @BeforeEach
    void setUp() {
        fallRule = new FallRule(3); // 3 seconds sustained low posture required
    }

    @Test
    @DisplayName("Sit-down false positive: UPRIGHT to SITTING must NOT trigger fall")
    void testSitDownFalsePositiveDoesNotTrigger() {
        Instant now = Instant.now();

        // 1. Person upright
        SensorEventDto uprightEvent = new SensorEventDto();
        uprightEvent.setType(EventType.POSE_STATE);
        uprightEvent.setPosture(Posture.UPRIGHT);
        uprightEvent.setTimestamp(now);
        assertFalse(fallRule.evaluate(uprightEvent).isTriggered());

        // 2. Person sits down
        SensorEventDto sitEvent = new SensorEventDto();
        sitEvent.setType(EventType.POSE_STATE);
        sitEvent.setPosture(Posture.SITTING);
        sitEvent.setTimestamp(now.plusSeconds(1));
        RuleDecision decision = fallRule.evaluate(sitEvent);

        assertFalse(decision.isTriggered(), "Sit-down must not trigger a fall alert!");
    }

    @Test
    @DisplayName("True positive: UPRIGHT to HORIZONTAL sustained >= 3s triggers FALL")
    void testRapidTransitionToHorizontalSustainedTriggers() {
        Instant now = Instant.now();

        // 1. Person upright
        SensorEventDto upright = new SensorEventDto();
        upright.setType(EventType.POSE_STATE);
        upright.setPosture(Posture.UPRIGHT);
        upright.setTimestamp(now);
        fallRule.evaluate(upright);

        // 2. Fall happens: horizontal posture at t = 1
        SensorEventDto horiz1 = new SensorEventDto();
        horiz1.setType(EventType.POSE_STATE);
        horiz1.setPosture(Posture.HORIZONTAL);
        horiz1.setTimestamp(now.plusSeconds(1));
        assertFalse(fallRule.evaluate(horiz1).isTriggered(), "Should wait for sustained low posture");

        // 3. Sustained horizontal at t = 4 (3 seconds elapsed)
        SensorEventDto horiz2 = new SensorEventDto();
        horiz2.setType(EventType.POSE_STATE);
        horiz2.setPosture(Posture.HORIZONTAL);
        horiz2.setTimestamp(now.plusSeconds(4));
        RuleDecision decision = fallRule.evaluate(horiz2);

        assertTrue(decision.isTriggered(), "Sustained horizontal posture must trigger fall alert");
        assertEquals(IncidentType.FALL, decision.getIncidentType());
    }

    @Test
    @DisplayName("Brief trip recovered within 1s must NOT trigger fall")
    void testBriefTripRecoveredDoesNotTrigger() {
        Instant now = Instant.now();

        SensorEventDto upright = new SensorEventDto();
        upright.setType(EventType.POSE_STATE);
        upright.setPosture(Posture.UPRIGHT);
        upright.setTimestamp(now);
        fallRule.evaluate(upright);

        SensorEventDto horiz = new SensorEventDto();
        horiz.setType(EventType.POSE_STATE);
        horiz.setPosture(Posture.HORIZONTAL);
        horiz.setTimestamp(now.plusSeconds(1));
        fallRule.evaluate(horiz);

        // Stood right back up at t = 2
        SensorEventDto recovered = new SensorEventDto();
        recovered.setType(EventType.POSE_STATE);
        recovered.setPosture(Posture.UPRIGHT);
        recovered.setTimestamp(now.plusSeconds(2));
        RuleDecision decision = fallRule.evaluate(recovered);

        assertFalse(decision.isTriggered(), "Recovered trip must not trigger fall");
    }
}
