package com.aegis.engine;

import com.aegis.dto.SensorEventDto;
import com.aegis.model.EventType;
import com.aegis.model.IncidentType;
import com.aegis.model.Severity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class NightWanderRuleTest {

    private NightWanderRule nightWanderRule;

    @BeforeEach
    void setUp() {
        nightWanderRule = new NightWanderRule("22:00", "06:00", 30); // 30s demo baseline
    }

    @Test
    @DisplayName("Tier 1: Bed exit during 3 AM demo triggers gentle voice prompt")
    void testBedExitDuringQuietHours() {
        SensorEventDto bedExit = new SensorEventDto();
        bedExit.setType(EventType.BED_EXIT);
        bedExit.setPretend3am(true);
        bedExit.setRoom("bedroom");
        bedExit.setTimestamp(Instant.now());

        RuleDecision decision = nightWanderRule.evaluate(bedExit);
        assertTrue(decision.isTriggered());
        assertEquals(IncidentType.NIGHT_WANDERING, decision.getIncidentType());
        assertEquals(Severity.INFO, decision.getSeverity());
        assertFalse(decision.isImmediateAlert());
        assertTrue(decision.getCheckInPrompt().contains("bathroom"));
    }

    @Test
    @DisplayName("Tier 2: Out of bed beyond baseline (or hallway trajectory) escalates to WARNING")
    void testNightWanderingHallwayEscalation() {
        Instant now = Instant.now();

        // 1. Bed exit at t = 0
        SensorEventDto bedExit = new SensorEventDto();
        bedExit.setType(EventType.BED_EXIT);
        bedExit.setPretend3am(true);
        bedExit.setTimestamp(now);
        nightWanderRule.evaluate(bedExit);

        // 2. Walking towards hallway door at t = 35s (> 30s baseline)
        SensorEventDto moveEvent = new SensorEventDto();
        moveEvent.setType(EventType.ROOM_CHANGE);
        moveEvent.setRoom("hallway");
        moveEvent.setPretend3am(true);
        moveEvent.setTimestamp(now.plusSeconds(35));

        RuleDecision decision = nightWanderRule.evaluate(moveEvent);
        assertTrue(decision.isTriggered());
        assertEquals(IncidentType.NIGHT_WANDERING, decision.getIncidentType());
        assertEquals(Severity.WARNING, decision.getSeverity());
    }

    @Test
    @DisplayName("Tier 3: Exterior door open during quiet hours triggers IMMEDIATE CRITICAL ALERT")
    void testDoorOpenDuringQuietHoursImmediateAlert() {
        SensorEventDto doorEvent = new SensorEventDto();
        doorEvent.setType(EventType.DOOR_OPEN);
        doorEvent.setRoom("front door");
        doorEvent.setPretend3am(true);
        doorEvent.setTimestamp(Instant.now());

        RuleDecision decision = nightWanderRule.evaluate(doorEvent);
        assertTrue(decision.isTriggered());
        assertEquals(IncidentType.DOOR_EXIT, decision.getIncidentType());
        assertEquals(Severity.CRITICAL, decision.getSeverity());
        assertTrue(decision.isImmediateAlert(), "Exterior door exit at night must be an immediate alert");
    }

    @Test
    @DisplayName("Daytime door open (pretend3am=false, at noon) must NOT trigger night wander alert")
    void testDaytimeDoorOpenDoesNotTrigger() {
        SensorEventDto doorEvent = new SensorEventDto();
        doorEvent.setType(EventType.DOOR_OPEN);
        doorEvent.setRoom("front door");
        doorEvent.setPretend3am(false);
        // Force timestamp to 12:00 PM UTC
        doorEvent.setTimestamp(Instant.parse("2026-10-14T12:00:00Z"));

        RuleDecision decision = nightWanderRule.evaluate(doorEvent);
        assertFalse(decision.isTriggered(), "Daytime door opening should not trigger night wander rule");
    }
}
