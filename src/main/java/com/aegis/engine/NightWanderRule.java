package com.aegis.engine;

import com.aegis.dto.SensorEventDto;
import com.aegis.model.EventType;
import com.aegis.model.IncidentType;
import com.aegis.model.Severity;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.concurrent.atomic.AtomicReference;

public class NightWanderRule {

    private final String quietHoursStart;
    private final String quietHoursEnd;
    private final int outOfBedBaselineSeconds;

    private final AtomicReference<Instant> bedExitTime = new AtomicReference<>(null);

    public NightWanderRule(String quietHoursStart, String quietHoursEnd, int outOfBedBaselineSeconds) {
        this.quietHoursStart = quietHoursStart;
        this.quietHoursEnd = quietHoursEnd;
        this.outOfBedBaselineSeconds = outOfBedBaselineSeconds;
    }

    public void clear() {
        this.bedExitTime.set(null);
    }

    public boolean isQuietHours(Instant timestamp, Boolean pretend3am) {
        if (Boolean.TRUE.equals(pretend3am)) {
            return true;
        }
        LocalTime now = (timestamp != null ? timestamp : Instant.now())
                .atZone(ZoneId.systemDefault())
                .toLocalTime();

        LocalTime start = LocalTime.parse(quietHoursStart);
        LocalTime end = LocalTime.parse(quietHoursEnd);

        if (start.isAfter(end)) {
            // Over midnight (e.g. 22:00 to 06:00)
            return now.isAfter(start) || now.isBefore(end);
        } else {
            return now.isAfter(start) && now.isBefore(end);
        }
    }

    public RuleDecision evaluate(SensorEventDto event) {
        if (event == null || event.getType() == null) {
            return RuleDecision.noTrigger();
        }

        boolean quiet = isQuietHours(event.getTimestamp(), event.getPretend3am());
        if (!quiet) {
            return RuleDecision.noTrigger();
        }

        Instant now = event.getTimestamp() != null ? event.getTimestamp() : Instant.now();

        // Tier 3: Front door or garage door opened during quiet hours -> IMMEDIATE URGENT ALERT
        if (event.getType() == EventType.DOOR_OPEN) {
            clear();
            String room = event.getRoom() != null ? event.getRoom() : "front door";
            return RuleDecision.immediateAlert(
                    IncidentType.DOOR_EXIT,
                    Severity.CRITICAL,
                    "URGENT: Exterior " + room + " opened during quiet hours. Immediate risk of night-time wandering outside."
            );
        }

        // Tier 1: Bed Exit during quiet hours -> gentle voice prompt (Tier 1)
        if (event.getType() == EventType.BED_EXIT) {
            bedExitTime.set(now);
            return RuleDecision.checkIn(
                    IncidentType.NIGHT_WANDERING,
                    Severity.INFO,
                    "Bed exit detected during quiet hours (3:00 AM demo). Gentle navigational prompt issued.",
                    "It is night time. Would you like to go to the bathroom? I will turn on the light."
            );
        }

        // Tier 2: Walking to door / Room change after bed exit or out of bed longer than baseline
        if (event.getType() == EventType.ROOM_CHANGE || "hallway".equalsIgnoreCase(event.getRoom()) || "doorway".equalsIgnoreCase(event.getRoom())) {
            Instant exitTime = bedExitTime.get();
            if (exitTime != null) {
                long duration = Math.abs(Duration.between(exitTime, now).getSeconds());
                if (duration >= outOfBedBaselineSeconds || "hallway".equalsIgnoreCase(event.getRoom()) || "doorway".equalsIgnoreCase(event.getRoom())) {
                    return RuleDecision.checkIn(
                            IncidentType.NIGHT_WANDERING,
                            Severity.WARNING,
                            "Patient has moved towards the exit hallway during quiet hours (" + duration + "s since bed exit). Monitoring trajectory.",
                            "Hi John, remember it is still night time. Do you need any assistance?"
                    );
                }
            }
        }

        return RuleDecision.noTrigger();
    }
}
