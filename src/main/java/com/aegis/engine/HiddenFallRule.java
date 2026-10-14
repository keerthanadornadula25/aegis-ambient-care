package com.aegis.engine;

import com.aegis.dto.SensorEventDto;
import com.aegis.model.EventType;
import com.aegis.model.IncidentType;
import com.aegis.model.Posture;
import com.aegis.model.Severity;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;

public class HiddenFallRule {

    private final int impactWindowSeconds;
    private final AtomicReference<Instant> lastImpactTime = new AtomicReference<>(null);
    private final AtomicReference<String> lastImpactRoom = new AtomicReference<>("bathroom");

    public HiddenFallRule(int impactWindowSeconds) {
        this.impactWindowSeconds = impactWindowSeconds;
    }

    public void registerImpact(Instant time, String room) {
        this.lastImpactTime.set(time != null ? time : Instant.now());
        if (room != null && !room.isBlank()) {
            this.lastImpactRoom.set(room);
        }
    }

    public void clear() {
        this.lastImpactTime.set(null);
    }

    public RuleDecision evaluate(SensorEventDto event) {
        if (event == null) {
            return RuleDecision.noTrigger();
        }

        // Direct simulation shortcut
        if (event.getType() == EventType.SIMULATED_FALL) {
            String room = event.getRoom() != null ? event.getRoom() : "bathroom";
            return RuleDecision.checkIn(
                    IncidentType.HIDDEN_FALL_SUSPECTED,
                    Severity.WARNING,
                    "Possible hidden fall in the " + room + ". A loud impact sound was detected followed by sudden disappearance from view.",
                    "Hi John, are you okay? Say yes, or press the button."
            );
        }

        // Step 1: Record impact sound
        if (event.getType() == EventType.IMPACT_SOUND || (event.getSoundLevel() != null && event.getSoundLevel() > 0.85)) {
            registerImpact(event.getTimestamp(), event.getRoom());
            return RuleDecision.noTrigger();
        }

        // Step 2: Check for disappearance or horizontal posture within window
        Instant impact = lastImpactTime.get();
        if (impact != null) {
            Instant current = event.getTimestamp() != null ? event.getTimestamp() : Instant.now();
            long diffSeconds = Math.abs(Duration.between(impact, current).getSeconds());

            if (diffSeconds <= impactWindowSeconds) {
                if (event.getType() == EventType.PERSON_LOST || event.getPosture() == Posture.HORIZONTAL) {
                    clear();
                    String room = event.getRoom() != null ? event.getRoom() : lastImpactRoom.get();
                    return RuleDecision.checkIn(
                            IncidentType.HIDDEN_FALL_SUSPECTED,
                            Severity.WARNING,
                            "Possible hidden fall in the " + room + ". Impact sound followed by loss of camera visibility within " + diffSeconds + "s. (Simulated sensor prototype)",
                            "Hi John, are you okay? Say yes, or press the button."
                    );
                }
            } else {
                // Window expired
                clear();
            }
        }

        return RuleDecision.noTrigger();
    }
}
