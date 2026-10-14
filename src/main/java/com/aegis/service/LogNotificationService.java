package com.aegis.service;

import com.aegis.model.Incident;
import com.aegis.model.Patient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

@Service
@Primary
@ConditionalOnProperty(name = "aegis.twilio.enabled", havingValue = "false", matchIfMissing = true)
public class LogNotificationService implements NotificationService {

    private static final Logger log = LoggerFactory.getLogger(LogNotificationService.class);

    @Override
    public void sendAlert(Patient patient, Incident incident) {
        String patientName = patient != null ? patient.getName() : "Unknown Patient";
        String contactName = patient != null ? patient.getEmergencyContactName() : "Caregiver";
        String contactPhone = patient != null ? patient.getEmergencyContactPhone() : "N/A";
        String address = patient != null ? patient.getAddress() : "123 Main St";

        log.warn("""
                
                ================================================================================
                [SMS DISPATCH - DEMO LOG MODE (No API Key Required)]
                Recipient: {} [{}]
                Emergency Type: {} ({})
                Summary: {}
                Location: Room '{}' at {}
                Patient: {} (Allergies: {}, Condition: {})
                Status: CRITICAL ALERT escalated by Aegis Ambient State Machine
                ================================================================================
                """,
                contactName, contactPhone,
                incident.getType(), incident.getSeverity(),
                incident.getSummary(),
                incident.getRoom(), address,
                patientName,
                patient != null ? patient.getAllergies() : "None",
                patient != null ? patient.getCondition() : "None"
        );
    }
}
