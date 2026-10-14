package com.aegis.dto;

import com.aegis.model.Incident;
import com.aegis.model.IncidentStatus;
import com.aegis.model.IncidentType;
import com.aegis.model.Severity;
import java.time.Instant;

public class IncidentDto {

    private Long id;
    private Long patientId;
    private IncidentType type;
    private IncidentStatus status;
    private Severity severity;
    private String summary;
    private String room;
    private Instant detectedAt;
    private Instant resolvedAt;
    private Instant cancelledAt;
    private String notes;

    public IncidentDto() {}

    public IncidentDto(Incident incident) {
        this.id = incident.getId();
        this.patientId = incident.getPatientId();
        this.type = incident.getType();
        this.status = incident.getStatus();
        this.severity = incident.getSeverity();
        this.summary = incident.getSummary();
        this.room = incident.getRoom();
        this.detectedAt = incident.getDetectedAt();
        this.resolvedAt = incident.getResolvedAt();
        this.cancelledAt = incident.getCancelledAt();
        this.notes = incident.getNotes();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getPatientId() { return patientId; }
    public void setPatientId(Long patientId) { this.patientId = patientId; }
    public IncidentType getType() { return type; }
    public void setType(IncidentType type) { this.type = type; }
    public IncidentStatus getStatus() { return status; }
    public void setStatus(IncidentStatus status) { this.status = status; }
    public Severity getSeverity() { return severity; }
    public void setSeverity(Severity severity) { this.severity = severity; }
    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }
    public String getRoom() { return room; }
    public void setRoom(String room) { this.room = room; }
    public Instant getDetectedAt() { return detectedAt; }
    public void setDetectedAt(Instant detectedAt) { this.detectedAt = detectedAt; }
    public Instant getResolvedAt() { return resolvedAt; }
    public void setResolvedAt(Instant resolvedAt) { this.resolvedAt = resolvedAt; }
    public Instant getCancelledAt() { return cancelledAt; }
    public void setCancelledAt(Instant cancelledAt) { this.cancelledAt = cancelledAt; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}
