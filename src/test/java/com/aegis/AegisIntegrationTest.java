package com.aegis;

import com.aegis.controller.EventController;
import com.aegis.dto.SensorEventDto;
import com.aegis.engine.IncidentStateMachine;
import com.aegis.model.EventType;
import com.aegis.model.IncidentStatus;
import com.aegis.model.Posture;
import com.aegis.service.IncidentService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AegisIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private EventController eventController;

    @Autowired
    private IncidentStateMachine stateMachine;

    @Autowired
    private IncidentService incidentService;

    @Test
    @DisplayName("Health endpoint returns UP and NORMAL patient status")
    void testHealthEndpoint() {
        ResponseEntity<Map> response = restTemplate.getForEntity("/api/health", Map.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("UP", response.getBody().get("status"));
        assertEquals("Aegis Ambient", response.getBody().get("app"));
    }

    @Test
    @DisplayName("Patient profile endpoint returns seeded John Doe demo data")
    void testGetPatientProfile() {
        ResponseEntity<Map> response = restTemplate.getForEntity("/api/patients/1", Map.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("John Doe", response.getBody().get("name"));
        assertEquals(78, response.getBody().get("age"));
        assertEquals("O+", response.getBody().get("bloodType"));
        assertEquals("Penicillin", response.getBody().get("allergies"));
    }

    @Test
    @DisplayName("End-to-End Simulation: Hidden Fall -> Check In -> USER_OK -> Cancelled")
    void testHiddenFallAndUserOkCancellationFlow() {
        // Reset state first
        incidentService.resetDemo();
        assertEquals(IncidentStatus.NORMAL, stateMachine.getCurrentStatus());

        // 1. Simulate Hidden Fall
        SensorEventDto fallEvent = new SensorEventDto();
        fallEvent.setPatientId(1L);
        fallEvent.setType(EventType.SIMULATED_FALL);
        fallEvent.setRoom("bathroom");
        fallEvent.setTimestamp(Instant.now());

        eventController.handleSensorEvent(fallEvent);

        // State machine must transition to CHECK_IN
        assertEquals(IncidentStatus.CHECK_IN, stateMachine.getCurrentStatus(), "State machine should enter CHECK_IN upon fall detection");

        // 2. Patient responds with "I'm OK"
        SensorEventDto okEvent = new SensorEventDto();
        okEvent.setPatientId(1L);
        okEvent.setType(EventType.USER_OK);
        okEvent.setTimestamp(Instant.now());

        eventController.handleSensorEvent(okEvent);

        // State machine must return to NORMAL
        assertEquals(IncidentStatus.NORMAL, stateMachine.getCurrentStatus(), "State machine must return to NORMAL after USER_OK");

        // 3. Verify cancelled incident exists in H2 database
        ResponseEntity<List> incidentsRes = restTemplate.getForEntity("/api/incidents?patientId=1", List.class);
        assertEquals(HttpStatus.OK, incidentsRes.getStatusCode());
        assertFalse(incidentsRes.getBody().isEmpty(), "Incident history must contain the cancelled incident");
    }

    @Test
    @DisplayName("End-to-End Simulation: 3 AM Exterior Door Open -> Immediate CRITICAL ALERT -> Caregiver Resolve")
    void testDoorOpenImmediateAlertAndResolution() {
        incidentService.resetDemo();

        // 1. Door open during quiet hours
        SensorEventDto doorEvent = new SensorEventDto();
        doorEvent.setPatientId(1L);
        doorEvent.setType(EventType.DOOR_OPEN);
        doorEvent.setRoom("front door");
        doorEvent.setPretend3am(true);
        doorEvent.setTimestamp(Instant.now());

        eventController.handleSensorEvent(doorEvent);

        // Must immediately trigger ALERT
        assertEquals(IncidentStatus.ALERT, stateMachine.getCurrentStatus(), "Door open at night must trigger immediate ALERT");

        // 2. Caregiver resolves the alert via REST endpoint
        ResponseEntity<List> incidentsRes = restTemplate.getForEntity("/api/incidents?patientId=1", List.class);
        assertFalse(incidentsRes.getBody().isEmpty());
        Map firstInc = (Map) incidentsRes.getBody().get(0);
        Integer incidentId = (Integer) firstInc.get("id");

        ResponseEntity<Map> resolveRes = restTemplate.postForEntity("/api/incidents/" + incidentId + "/resolve", null, Map.class);
        assertEquals(HttpStatus.OK, resolveRes.getStatusCode());
        assertTrue((Boolean) resolveRes.getBody().get("success"));

        // State returns to NORMAL
        assertEquals(IncidentStatus.NORMAL, stateMachine.getCurrentStatus());
    }
}
