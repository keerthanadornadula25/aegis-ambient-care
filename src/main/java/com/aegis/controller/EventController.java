package com.aegis.controller;

import com.aegis.dto.SensorEventDto;
import com.aegis.engine.IncidentStateMachine;
import com.aegis.engine.RuleDecision;
import com.aegis.engine.RuleEngine;
import com.aegis.model.EventType;
import com.aegis.model.SensorEvent;
import com.aegis.repo.SensorEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;

import java.time.Instant;

@Controller
public class EventController {

    private static final Logger log = LoggerFactory.getLogger(EventController.class);

    private final RuleEngine ruleEngine;
    private final IncidentStateMachine stateMachine;
    private final SensorEventRepository sensorEventRepository;

    public EventController(RuleEngine ruleEngine,
                           IncidentStateMachine stateMachine,
                           SensorEventRepository sensorEventRepository) {
        this.ruleEngine = ruleEngine;
        this.stateMachine = stateMachine;
        this.sensorEventRepository = sensorEventRepository;
    }

    @MessageMapping("/event")
    public void handleSensorEvent(SensorEventDto event) {
        if (event == null) {
            return;
        }

        Long patientId = event.getPatientId() != null ? event.getPatientId() : 1L;

        // Persist metadata (Never stores raw images or audio)
        try {
            SensorEvent entity = new SensorEvent(
                    patientId,
                    event.getType(),
                    event.getPosture(),
                    event.getSwayScore(),
                    event.getStillSeconds(),
                    event.getSoundLevel(),
                    event.getRoom(),
                    event.getSimulated(),
                    event.getTimestamp() != null ? event.getTimestamp() : Instant.now()
            );
            sensorEventRepository.save(entity);
        } catch (Exception e) {
            log.error("Failed to save sensor event metadata: {}", e.getMessage());
        }

        // Branch 1: User verification response
        if (event.getType() == EventType.USER_OK) {
            log.info("Processing USER_OK event for patient {}", patientId);
            stateMachine.handleUserOk(patientId);
            return;
        }

        // Branch 2: Evaluate sensor state against rule engine
        RuleDecision decision = ruleEngine.evaluate(event);
        if (decision.isTriggered()) {
            log.info("Rule triggered: {} with severity {}", decision.getIncidentType(), decision.getSeverity());
            stateMachine.processDecision(patientId, decision, event.getRoom());
        }
    }
}
