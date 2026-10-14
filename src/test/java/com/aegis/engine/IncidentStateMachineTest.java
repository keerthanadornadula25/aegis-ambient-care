package com.aegis.engine;

import com.aegis.config.AegisProperties;
import com.aegis.model.*;
import com.aegis.repo.IncidentRepository;
import com.aegis.repo.PatientRepository;
import com.aegis.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.messaging.simp.SimpMessageSendingOperations;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class IncidentStateMachineTest {

    private AegisProperties properties;
    private IncidentRepository incidentRepository;
    private PatientRepository patientRepository;
    private NotificationService notificationService;
    private SimpMessageSendingOperations messagingTemplate;
    private IncidentStateMachine stateMachine;

    @BeforeEach
    void setUp() {
        properties = new AegisProperties();
        properties.setDemoMode(true);
        properties.setDemoCheckInSeconds(2);
        properties.setDemoCountdownSeconds(2);
        properties.setCooldownSeconds(0); // 0 for test speed

        incidentRepository = mock(IncidentRepository.class);
        patientRepository = mock(PatientRepository.class);
        notificationService = mock(NotificationService.class);
        messagingTemplate = mock(SimpMessageSendingOperations.class);

        when(incidentRepository.save(any(Incident.class))).thenAnswer(invocation -> {
            Incident inc = invocation.getArgument(0);
            if (inc.getId() == null) {
                inc.setId(101L);
            }
            return inc;
        });

        Patient patient = new Patient(1L, "John Doe", 78, "O+", "Penicillin", "Diabetes", "123 Main St", "Jane Doe", "555");
        when(patientRepository.findById(1L)).thenReturn(Optional.of(patient));

        stateMachine = new IncidentStateMachine(
                properties,
                incidentRepository,
                patientRepository,
                notificationService,
                messagingTemplate
        );
    }

    @Test
    @DisplayName("Verification flow: decision triggers CHECK_IN status")
    void testProcessDecisionStartsCheckIn() {
        RuleDecision decision = RuleDecision.checkIn(
                IncidentType.HIDDEN_FALL_SUSPECTED,
                Severity.WARNING,
                "Possible hidden fall in bathroom",
                "Hi John, are you okay?"
        );

        stateMachine.processDecision(1L, decision, "bathroom");

        assertEquals(IncidentStatus.CHECK_IN, stateMachine.getCurrentStatus());
        verify(incidentRepository, atLeastOnce()).save(any(Incident.class));
        verify(messagingTemplate, atLeastOnce()).convertAndSend(eq("/topic/patient/1/commands"), any(Object.class));
    }

    @Test
    @DisplayName("Cancelled alert: USER_OK returns state machine to NORMAL and cancels false alarm")
    void testUserOkCancelsAlert() {
        RuleDecision decision = RuleDecision.checkIn(
                IncidentType.PROLONGED_STILLNESS,
                Severity.WARNING,
                "Prolonged stillness detected",
                "Hi John, are you okay?"
        );

        stateMachine.processDecision(1L, decision, "living room");
        assertEquals(IncidentStatus.CHECK_IN, stateMachine.getCurrentStatus());

        // Patient confirms they are OK
        stateMachine.handleUserOk(1L);

        assertEquals(IncidentStatus.NORMAL, stateMachine.getCurrentStatus(), "State must return to NORMAL after USER_OK");
        verify(messagingTemplate, atLeastOnce()).convertAndSend(eq("/topic/caregiver/1/status"), any(Object.class));
    }

    @Test
    @DisplayName("Immediate alert: exterior door exit immediately triggers ALERT and calls notification service")
    void testImmediateAlertDispatchesDirectly() {
        RuleDecision decision = RuleDecision.immediateAlert(
                IncidentType.DOOR_EXIT,
                Severity.CRITICAL,
                "Exterior door opened at 3 AM"
        );

        stateMachine.processDecision(1L, decision, "front door");

        assertEquals(IncidentStatus.ALERT, stateMachine.getCurrentStatus());
        verify(notificationService, times(1)).sendAlert(any(), any());
    }
}
