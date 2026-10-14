package com.aegis.engine;

import com.aegis.config.AegisProperties;
import com.aegis.dto.CaregiverStatusDto;
import com.aegis.dto.PatientCommandDto;
import com.aegis.model.*;
import com.aegis.repo.IncidentRepository;
import com.aegis.repo.PatientRepository;
import com.aegis.service.NotificationService;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.*;

@Component
public class IncidentStateMachine {

    private static final Logger log = LoggerFactory.getLogger(IncidentStateMachine.class);

    private final AegisProperties properties;
    private final IncidentRepository incidentRepository;
    private final PatientRepository patientRepository;
    private final NotificationService notificationService;
    private final SimpMessageSendingOperations messagingTemplate;

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);

    // Patient state cache (single patient #1 for demo)
    private volatile IncidentStatus currentStatus = IncidentStatus.NORMAL;
    private volatile Incident currentIncident = null;
    private volatile Instant lastCancelledTime = null;
    private volatile int countdownRemaining = 0;

    private ScheduledFuture<?> checkInFuture = null;
    private ScheduledFuture<?> countdownFuture = null;
    private ScheduledFuture<?> tickFuture = null;

    public IncidentStateMachine(AegisProperties properties,
                                IncidentRepository incidentRepository,
                                PatientRepository patientRepository,
                                NotificationService notificationService,
                                SimpMessageSendingOperations messagingTemplate) {
        this.properties = properties;
        this.incidentRepository = incidentRepository;
        this.patientRepository = patientRepository;
        this.notificationService = notificationService;
        this.messagingTemplate = messagingTemplate;
    }

    public synchronized IncidentStatus getCurrentStatus() {
        return currentStatus;
    }

    public synchronized CaregiverStatusDto getCurrentCaregiverStatus(Long patientId) {
        if (currentStatus == IncidentStatus.NORMAL || currentIncident == null) {
            return new CaregiverStatusDto(
                    patientId,
                    IncidentStatus.NORMAL,
                    null,
                    Severity.INFO,
                    "All clear. Ambient sensors active and observing locally.",
                    "home",
                    Instant.now(),
                    0,
                    null
            );
        }

        return new CaregiverStatusDto(
                patientId,
                currentStatus,
                currentIncident.getType(),
                currentIncident.getSeverity(),
                currentIncident.getSummary(),
                currentIncident.getRoom(),
                currentIncident.getDetectedAt(),
                countdownRemaining,
                currentIncident.getId()
        );
    }

    public synchronized void processDecision(Long patientId, RuleDecision decision, String room) {
        if (decision == null || !decision.isTriggered()) {
            return;
        }

        // Check cooldown guard for non-immediate alerts
        if (!decision.isImmediateAlert() && lastCancelledTime != null) {
            long secondsSinceCancel = Duration.between(lastCancelledTime, Instant.now()).getSeconds();
            if (secondsSinceCancel < properties.getCooldownSeconds()) {
                log.info("Ignoring non-critical trigger during cooldown window ({}s elapsed / {}s cooldown)",
                        secondsSinceCancel, properties.getCooldownSeconds());
                return;
            }
        }

        // Immediate Escalation (e.g. night wandering exit door)
        if (decision.isImmediateAlert()) {
            cancelAllTimers();
            Incident incident = new Incident(
                    patientId,
                    decision.getIncidentType(),
                    IncidentStatus.ALERT,
                    decision.getSeverity(),
                    decision.getSummary(),
                    room != null ? room : "front door",
                    Instant.now()
            );
            incident = incidentRepository.save(incident);
            this.currentIncident = incident;
            this.currentStatus = IncidentStatus.ALERT;
            this.countdownRemaining = 0;

            log.warn("IMMEDIATE ALERT TRIGGERED: {}", decision.getSummary());

            // Notify caregiver via WebSocket and SMS service
            broadcastCaregiverStatus(patientId);
            Patient patient = patientRepository.findById(patientId).orElse(null);
            notificationService.sendAlert(patient, incident);

            // Also notify patient page
            sendPatientCommand(patientId, new PatientCommandDto("SPEAK", "Urgent alert. Exterior door opened during quiet hours. Caregiver notified.", 0));
            return;
        }

        // Standard Two-stage verification: NORMAL -> CHECK_IN -> COUNTDOWN -> ALERT
        if (currentStatus == IncidentStatus.NORMAL) {
            cancelAllTimers();

            Incident incident = new Incident(
                    patientId,
                    decision.getIncidentType(),
                    IncidentStatus.CHECK_IN,
                    decision.getSeverity(),
                    decision.getSummary(),
                    room != null ? room : "bedroom",
                    Instant.now()
            );
            incident = incidentRepository.save(incident);
            this.currentIncident = incident;
            this.currentStatus = IncidentStatus.CHECK_IN;

            int checkInSec = properties.getEffectiveCheckInSeconds();
            this.countdownRemaining = checkInSec;

            log.info("Starting CHECK_IN verification flow for patient {} ({}s window)", patientId, checkInSec);

            // Broadcast CHECK_IN status to caregiver
            broadcastCaregiverStatus(patientId);

            // Speak prompt to patient
            String prompt = decision.getCheckInPrompt() != null ?
                    decision.getCheckInPrompt() : "Hi John, are you okay? Say yes, or press the button.";
            sendPatientCommand(patientId, new PatientCommandDto("SPEAK", prompt, checkInSec));

            // Schedule transition to COUNTDOWN
            final Long incId = incident.getId();
            checkInFuture = scheduler.schedule(() -> {
                synchronized (this) {
                    if (currentStatus == IncidentStatus.CHECK_IN && currentIncident != null && currentIncident.getId().equals(incId)) {
                        startCountdown(patientId, incId);
                    }
                }
            }, checkInSec, TimeUnit.SECONDS);
        }
    }

    private synchronized void startCountdown(Long patientId, Long incidentId) {
        if (currentStatus != IncidentStatus.CHECK_IN || currentIncident == null) {
            return;
        }

        int countdownSec = properties.getEffectiveCountdownSeconds();
        this.currentStatus = IncidentStatus.COUNTDOWN;
        this.countdownRemaining = countdownSec;

        currentIncident.setStatus(IncidentStatus.COUNTDOWN);
        currentIncident = incidentRepository.save(currentIncident);

        log.info("Transitioned to COUNTDOWN: {}s remaining before caregiver escalation", countdownSec);

        // Tell patient page to start countdown ring/timer
        sendPatientCommand(patientId, new PatientCommandDto("START_COUNTDOWN", "Verifying status. Escalating to caregiver in " + countdownSec + " seconds if unconfirmed.", countdownSec));
        broadcastCaregiverStatus(patientId);

        // Periodic tick every second to update countdownRemaining
        tickFuture = scheduler.scheduleAtFixedRate(() -> {
            synchronized (this) {
                if (currentStatus == IncidentStatus.COUNTDOWN && countdownRemaining > 0) {
                    countdownRemaining--;
                    broadcastCaregiverStatus(patientId);
                }
            }
        }, 1, 1, TimeUnit.SECONDS);

        // Schedule escalation to ALERT
        countdownFuture = scheduler.schedule(() -> {
            synchronized (this) {
                if (currentStatus == IncidentStatus.COUNTDOWN && currentIncident != null && currentIncident.getId().equals(incidentId)) {
                    escalateToAlert(patientId, incidentId);
                }
            }
        }, countdownSec, TimeUnit.SECONDS);
    }

    private synchronized void escalateToAlert(Long patientId, Long incidentId) {
        cancelAllTimers();

        if (currentIncident == null) return;

        this.currentStatus = IncidentStatus.ALERT;
        this.countdownRemaining = 0;
        currentIncident.setStatus(IncidentStatus.ALERT);
        currentIncident.setSeverity(Severity.CRITICAL);
        currentIncident = incidentRepository.save(currentIncident);

        log.warn("ESCALATED TO CRITICAL ALERT for patient {}: {}", patientId, currentIncident.getSummary());

        broadcastCaregiverStatus(patientId);

        Patient patient = patientRepository.findById(patientId).orElse(null);
        notificationService.sendAlert(patient, currentIncident);

        sendPatientCommand(patientId, new PatientCommandDto("SPEAK", "Emergency alert dispatched to caregiver.", 0));
    }

    public synchronized void handleUserOk(Long patientId) {
        log.info("Received USER_OK from patient {}", patientId);

        if (currentStatus == IncidentStatus.CHECK_IN || currentStatus == IncidentStatus.COUNTDOWN) {
            cancelAllTimers();

            if (currentIncident != null) {
                currentIncident.setStatus(IncidentStatus.CANCELLED);
                currentIncident.setCancelledAt(Instant.now());
                currentIncident.setNotes("Cancelled by patient via 'I'm OK' button or voice confirmation.");
                incidentRepository.save(currentIncident);
            }

            this.currentStatus = IncidentStatus.NORMAL;
            this.currentIncident = null;
            this.countdownRemaining = 0;
            this.lastCancelledTime = Instant.now();

            // Notify patient page to cancel alarm/countdown
            sendPatientCommand(patientId, new PatientCommandDto("CANCEL", "Check-in confirmed: All clear. Glad you are okay!", 0));

            // Notify caregiver that patient cancelled false alarm
            CaregiverStatusDto okStatus = new CaregiverStatusDto(
                    patientId,
                    IncidentStatus.NORMAL,
                    null,
                    Severity.INFO,
                    "Alert cancelled by patient response ('I'm OK'). System returned to normal monitoring.",
                    "home",
                    Instant.now(),
                    0,
                    null
            );
            messagingTemplate.convertAndSend("/topic/caregiver/" + patientId + "/status", okStatus);
        } else if (currentStatus == IncidentStatus.ALERT) {
            // Patient presses I'm OK while in ALERT state
            sendPatientCommand(patientId, new PatientCommandDto("SPEAK", "Your caregiver has already been notified. They can resolve the alert from their dashboard.", 0));
        }
    }

    public synchronized boolean resolveIncident(Long patientId, Long incidentId) {
        cancelAllTimers();

        Incident target = null;
        if (incidentId != null) {
            target = incidentRepository.findById(incidentId).orElse(null);
        } else if (currentIncident != null) {
            target = currentIncident;
        }

        if (target != null) {
            target.setStatus(IncidentStatus.RESOLVED);
            target.setResolvedAt(Instant.now());
            incidentRepository.save(target);
        }

        this.currentStatus = IncidentStatus.NORMAL;
        this.currentIncident = null;
        this.countdownRemaining = 0;

        broadcastCaregiverStatus(patientId);
        sendPatientCommand(patientId, new PatientCommandDto("CLEAR", "Incident resolved by caregiver.", 0));
        log.info("Incident {} resolved by caregiver for patient {}", incidentId, patientId);
        return true;
    }

    public synchronized void resetDemo(Long patientId) {
        cancelAllTimers();
        this.currentStatus = IncidentStatus.NORMAL;
        this.currentIncident = null;
        this.countdownRemaining = 0;
        this.lastCancelledTime = null;

        broadcastCaregiverStatus(patientId);
        sendPatientCommand(patientId, new PatientCommandDto("CLEAR", "Demo state reset.", 0));
        log.info("Reset demo state for patient {}", patientId);
    }

    private void cancelAllTimers() {
        if (checkInFuture != null) {
            checkInFuture.cancel(true);
            checkInFuture = null;
        }
        if (countdownFuture != null) {
            countdownFuture.cancel(true);
            countdownFuture = null;
        }
        if (tickFuture != null) {
            tickFuture.cancel(true);
            tickFuture = null;
        }
    }

    private void broadcastCaregiverStatus(Long patientId) {
        CaregiverStatusDto dto = getCurrentCaregiverStatus(patientId);
        messagingTemplate.convertAndSend("/topic/caregiver/" + patientId + "/status", dto);
    }

    private void sendPatientCommand(Long patientId, PatientCommandDto command) {
        messagingTemplate.convertAndSend("/topic/patient/" + patientId + "/commands", command);
    }

    @PreDestroy
    public void cleanup() {
        cancelAllTimers();
        scheduler.shutdownNow();
    }
}
