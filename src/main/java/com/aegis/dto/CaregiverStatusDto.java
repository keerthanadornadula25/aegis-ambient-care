package com.aegis.dto;

import com.aegis.model.IncidentStatus;
import com.aegis.model.IncidentType;
import com.aegis.model.Severity;
import java.time.Instant;

public class CaregiverStatusDto {

    private Long patientId;
    private IncidentStatus status;
    private IncidentType incidentType;
    private Severity severity;
    private String summary;
    private String room;
    private Instant timestamp;
    private Integer countdownRemaining;
    private Long incidentId;

    public CaregiverStatusDto() {
        this.timestamp = Instant.now();
    }

    public CaregiverStatusDto(Long patientId, IncidentStatus status, IncidentType incidentType,
                              Severity severity, String summary, String room, Instant timestamp,
                              Integer countdownRemaining, Long incidentId) {
        this.patientId = patientId;
        this.status = status;
        this.incidentType = incidentType;
        this.severity = severity;
        this.summary = summary;
        this.room = room;
        this.timestamp = timestamp;
        this.countdownRemaining = countdownRemaining;
        this.incidentId = incidentId;
    }

    public Long getPatientId() { return patientId; }
    public void setPatientId(Long patientId) { this.patientId = patientId; }

    public IncidentStatus getStatus() { return status; }
    public void setStatus(IncidentStatus status) { this.status = status; }

    public IncidentType getIncidentType() { return incidentType; }
    public void setIncidentType(IncidentType incidentType) { this.incidentType = incidentType; }

    public Severity getSeverity() { return severity; }
    public void setSeverity(Severity severity) { this.severity = severity; }

    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }

    public String getRoom() { return room; }
    public void setRoom(String room) { this.room = room; }

    public Instant getTimestamp() { return timestamp; }
    public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }

    public Integer getCountdownRemaining() { return countdownRemaining; }
    public void setCountdownRemaining(Integer countdownRemaining) { this.countdownRemaining = countdownRemaining; }

    public Long getIncidentId() { return incidentId; }
    public void setIncidentId(Long incidentId) { this.incidentId = incidentId; }
}
