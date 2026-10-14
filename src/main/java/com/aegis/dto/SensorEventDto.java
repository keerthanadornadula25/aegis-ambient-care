package com.aegis.dto;

import com.aegis.model.EventType;
import com.aegis.model.Posture;
import java.time.Instant;

public class SensorEventDto {

    private Long patientId = 1L;
    private EventType type;
    private Posture posture = Posture.UNKNOWN;
    private Double swayScore = 0.0;
    private Integer stillSeconds = 0;
    private Double soundLevel = 0.0;
    private String room = "bedroom";
    private Boolean simulated = false;
    private Instant timestamp;
    private Boolean pretend3am = false;

    public SensorEventDto() {
        this.timestamp = Instant.now();
    }

    public Long getPatientId() { return patientId; }
    public void setPatientId(Long patientId) { this.patientId = patientId; }

    public EventType getType() { return type; }
    public void setType(EventType type) { this.type = type; }

    public Posture getPosture() { return posture; }
    public void setPosture(Posture posture) { this.posture = posture; }

    public Double getSwayScore() { return swayScore; }
    public void setSwayScore(Double swayScore) { this.swayScore = swayScore; }

    public Integer getStillSeconds() { return stillSeconds; }
    public void setStillSeconds(Integer stillSeconds) { this.stillSeconds = stillSeconds; }

    public Double getSoundLevel() { return soundLevel; }
    public void setSoundLevel(Double soundLevel) { this.soundLevel = soundLevel; }

    public String getRoom() { return room; }
    public void setRoom(String room) { this.room = room; }

    public Boolean getSimulated() { return simulated; }
    public void setSimulated(Boolean simulated) { this.simulated = simulated; }

    public Instant getTimestamp() { return timestamp; }
    public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }

    public Boolean getPretend3am() { return pretend3am; }
    public void setPretend3am(Boolean pretend3am) { this.pretend3am = pretend3am; }
}
