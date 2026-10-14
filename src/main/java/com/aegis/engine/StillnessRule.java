package com.aegis.engine;

import com.aegis.dto.SensorEventDto;
import com.aegis.model.EventType;
import com.aegis.model.IncidentType;
import com.aegis.model.Posture;
import com.aegis.model.Severity;

public class StillnessRule {

    private final int stillnessThresholdSeconds;
    private final double swayThreshold;

    public StillnessRule(int stillnessThresholdSeconds, double swayThreshold) {
        this.stillnessThresholdSeconds = stillnessThresholdSeconds;
        this.swayThreshold = swayThreshold;
    }

    public RuleDecision evaluate(SensorEventDto event) {
        if (event == null) {
            return RuleDecision.noTrigger();
        }

        // Direct simulation button trigger
        if (event.getType() == EventType.SIMULATED_FREEZE) {
            String room = event.getRoom() != null ? event.getRoom() : "living room";
            return RuleDecision.checkIn(
                    IncidentType.PROLONGED_STILLNESS,
                    Severity.WARNING,
                    "Unusual prolonged stillness detected in the " + room + ". Body sway variance below baseline threshold for " + stillnessThresholdSeconds + "s. Checking on patient.",
                    "Hi John, are you okay? Say yes, or press the button."
            );
        }

        // Real pose tracking stillness
        if (event.getPosture() == Posture.UPRIGHT || event.getPosture() == Posture.SITTING) {
            int stillSeconds = event.getStillSeconds() != null ? event.getStillSeconds() : 0;
            double sway = event.getSwayScore() != null ? event.getSwayScore() : 1.0;

            if (stillSeconds >= stillnessThresholdSeconds && sway < swayThreshold) {
                String room = event.getRoom() != null ? event.getRoom() : "living room";
                return RuleDecision.checkIn(
                        IncidentType.PROLONGED_STILLNESS,
                        Severity.WARNING,
                        "Unusual prolonged stillness detected in the " + room + ". Patient remained stationary for " + stillSeconds + "s with sway score " + String.format("%.3f", sway) + ". Checking on patient.",
                        "Hi John, are you okay? Say yes, or press the button."
                );
            }
        }

        return RuleDecision.noTrigger();
    }
}
