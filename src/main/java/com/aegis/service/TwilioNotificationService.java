package com.aegis.service;

import com.aegis.config.AegisProperties;
import com.aegis.model.Incident;
import com.aegis.model.Patient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(name = "aegis.twilio.enabled", havingValue = "true")
public class TwilioNotificationService implements NotificationService {

    private static final Logger log = LoggerFactory.getLogger(TwilioNotificationService.class);
    private final AegisProperties properties;

    public TwilioNotificationService(AegisProperties properties) {
        this.properties = properties;
    }

    @Override
    public void sendAlert(Patient patient, Incident incident) {
        String sid = properties.getTwilio().getAccountSid();
        String token = properties.getTwilio().getAuthToken();
        String from = properties.getTwilio().getFromNumber();
        String to = properties.getTwilio().getToNumber();

        if (sid == null || sid.isBlank() || token == null || token.isBlank()) {
            log.warn("Twilio enabled but credentials missing. Falling back to log SMS mode for Incident #{}", incident.getId());
            return;
        }

        try {
            log.info("Sending Twilio SMS from {} to {} for Incident #{}", from, to, incident.getId());
            // If Twilio SDK is on classpath and credentials valid, dispatch:
            com.twilio.Twilio.init(sid, token);
            com.twilio.rest.api.v2010.account.Message.creator(
                    new com.twilio.type.PhoneNumber(to),
                    new com.twilio.type.PhoneNumber(from),
                    "CRITICAL ALERT - Aegis Ambient: " + incident.getSummary()
            ).create();
            log.info("Twilio SMS sent successfully for Incident #{}", incident.getId());
        } catch (Exception ex) {
            log.error("Failed to send Twilio SMS: {}. Please verify Twilio credentials.", ex.getMessage());
        }
    }
}
