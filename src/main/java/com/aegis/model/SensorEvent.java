package com.aegis.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "sensor_events")
public class SensorEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long patientId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EventType type;

    @Enumerated(EnumType.STRING)
    private Posture posture;

    private Double swayScore;

    private Integer stillSeconds;

    private Double soundLevel;

    private String room;

    private Boolean simulated;

    @Column(nullable = false)
    private Instant timestamp;

    public SensorEvent() {}

    public SensorEvent(Long patientId, EventType type, Posture posture, Double swayScore, Integer stillSeconds, Double soundLevel, String room, Boolean simulated, Instant timestamp) {
        this.patientId = patientId;
        this.type = type;
        this.posture = posture;
        this.swayScore = swayScore;
        this.stillSeconds = stillSeconds;
        this.soundLevel = soundLevel;
        this.room = room;
        this.simulated = simulated;
        this.timestamp = timestamp;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

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
}
