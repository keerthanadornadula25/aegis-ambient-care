package com.aegis.engine;

import com.aegis.model.IncidentType;
import com.aegis.model.Severity;

public class RuleDecision {

    private final boolean triggered;
    private final IncidentType incidentType;
    private final Severity severity;
    private final String summary;
    private final String checkInPrompt;
    private final boolean immediateAlert;

    public RuleDecision(boolean triggered, IncidentType incidentType, Severity severity,
                        String summary, String checkInPrompt, boolean immediateAlert) {
        this.triggered = triggered;
        this.incidentType = incidentType;
        this.severity = severity;
        this.summary = summary;
        this.checkInPrompt = checkInPrompt;
        this.immediateAlert = immediateAlert;
    }

    public static RuleDecision noTrigger() {
        return new RuleDecision(false, null, Severity.INFO, null, null, false);
    }

    public static RuleDecision checkIn(IncidentType incidentType, Severity severity, String summary, String checkInPrompt) {
        return new RuleDecision(true, incidentType, severity, summary, checkInPrompt, false);
    }

    public static RuleDecision immediateAlert(IncidentType incidentType, Severity severity, String summary) {
        return new RuleDecision(true, incidentType, severity, summary, null, true);
    }

    public boolean isTriggered() { return triggered; }
    public IncidentType getIncidentType() { return incidentType; }
    public Severity getSeverity() { return severity; }
    public String getSummary() { return summary; }
    public String getCheckInPrompt() { return checkInPrompt; }
    public boolean isImmediateAlert() { return immediateAlert; }
}
