package com.aegis.service;

import com.aegis.model.Incident;
import com.aegis.model.Patient;

public interface NotificationService {
    void sendAlert(Patient patient, Incident incident);
}
