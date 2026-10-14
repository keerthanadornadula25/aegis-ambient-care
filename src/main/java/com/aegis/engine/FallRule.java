package com.aegis.engine;

import com.aegis.dto.SensorEventDto;
import com.aegis.model.IncidentType;
import com.aegis.model.Posture;
import com.aegis.model.Severity;

import java.time.Duration;
import java.time.Instant;

public class FallRule {

    private final int requiredLowPostureSeconds;
    private Posture previousPosture = Posture.UNKNOWN;
    private Instant transitionToHorizontalTime = null;

    public FallRule(int requiredLowPostureSeconds) {
        this.requiredLowPostureSeconds = requiredLowPostureSeconds;
    }

    public void reset() {
        this.previousPosture = Posture.UNKNOWN;
        this.transitionToHorizontalTime = null;
    }

    public RuleDecision evaluate(SensorEventDto event) {
        if (event == null || event.getPosture() == null) {
            return RuleDecision.noTrigger();
        }

        Posture current = event.getPosture();
        Instant now = event.getTimestamp() != null ? event.getTimestamp() : Instant.now();

        // Check for normal sitting down: transition from UPRIGHT to SITTING is a benign event
        if (current == Posture.SITTING) {
            previousPosture = Posture.SITTING;
            transitionToHorizontalTime = null;
            return RuleDecision.noTrigger();
        }

        // Transition from UPRIGHT/SITTING to HORIZONTAL
        if (current == Posture.HORIZONTAL) {
            if (previousPosture == Posture.UPRIGHT || previousPosture == Posture.SITTING) {
                if (transitionToHorizontalTime == null) {
                    transitionToHorizontalTime = now;
                }
            }

            if (transitionToHorizontalTime != null) {
                long duration = Math.abs(Duration.between(transitionToHorizontalTime, now).getSeconds());
                if (duration >= requiredLowPostureSeconds) {
                    reset();
                    String room = event.getRoom() != null ? event.getRoom() : "living room";
                    return RuleDecision.checkIn(
                            IncidentType.FALL,
                            Severity.WARNING,
                            "Rapid vertical-to-horizontal fall detected in the " + room + " with sustained floor posture for " + duration + "s.",
                            "Hi John, are you okay? Say yes, or press the button."
                    );
                }
            }
        } else {
            // Stood back up or unknown
            if (current == Posture.UPRIGHT) {
                previousPosture = Posture.UPRIGHT;
                transitionToHorizontalTime = null;
            }
        }

        return RuleDecision.noTrigger();
    }
}
