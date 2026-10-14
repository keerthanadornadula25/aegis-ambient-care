package com.aegis.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "aegis")
public class AegisProperties {

    private boolean demoMode = true;
    private int checkInSeconds = 15;
    private int demoCheckInSeconds = 6;
    private int countdownSeconds = 30;
    private int demoCountdownSeconds = 15;
    private int cooldownSeconds = 60;

    private int stillnessThresholdSeconds = 120;
    private int demoStillnessThresholdSeconds = 15;
    private double swayThreshold = 0.020;

    private String quietHoursStart = "22:00";
    private String quietHoursEnd = "06:00";
    private int outOfBedBaselineSeconds = 600;
    private int demoOutOfBedBaselineSeconds = 30;

    private int fallImpactWindowSeconds = 10;
    private int fallLowPostureSeconds = 3;

    private Twilio twilio = new Twilio();

    public static class Twilio {
        private boolean enabled = false;
        private String accountSid = "";
        private String authToken = "";
        private String fromNumber = "";
        private String toNumber = "";

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public String getAccountSid() { return accountSid; }
        public void setAccountSid(String accountSid) { this.accountSid = accountSid; }
        public String getAuthToken() { return authToken; }
        public void setAuthToken(String authToken) { this.authToken = authToken; }
        public String getFromNumber() { return fromNumber; }
        public void setFromNumber(String fromNumber) { this.fromNumber = fromNumber; }
        public String getToNumber() { return toNumber; }
        public void setToNumber(String toNumber) { this.toNumber = toNumber; }
    }

    public int getEffectiveCheckInSeconds() {
        return demoMode ? demoCheckInSeconds : checkInSeconds;
    }

    public int getEffectiveCountdownSeconds() {
        return demoMode ? demoCountdownSeconds : countdownSeconds;
    }

    public int getEffectiveStillnessSeconds() {
        return demoMode ? demoStillnessThresholdSeconds : stillnessThresholdSeconds;
    }

    public int getEffectiveOutOfBedSeconds() {
        return demoMode ? demoOutOfBedBaselineSeconds : outOfBedBaselineSeconds;
    }

    // Standard getters and setters
    public boolean isDemoMode() { return demoMode; }
    public void setDemoMode(boolean demoMode) { this.demoMode = demoMode; }
    public int getCheckInSeconds() { return checkInSeconds; }
    public void setCheckInSeconds(int checkInSeconds) { this.checkInSeconds = checkInSeconds; }
    public int getDemoCheckInSeconds() { return demoCheckInSeconds; }
    public void setDemoCheckInSeconds(int demoCheckInSeconds) { this.demoCheckInSeconds = demoCheckInSeconds; }
    public int getCountdownSeconds() { return countdownSeconds; }
    public void setCountdownSeconds(int countdownSeconds) { this.countdownSeconds = countdownSeconds; }
    public int getDemoCountdownSeconds() { return demoCountdownSeconds; }
    public void setDemoCountdownSeconds(int demoCountdownSeconds) { this.demoCountdownSeconds = demoCountdownSeconds; }
    public int getCooldownSeconds() { return cooldownSeconds; }
    public void setCooldownSeconds(int cooldownSeconds) { this.cooldownSeconds = cooldownSeconds; }
    public int getStillnessThresholdSeconds() { return stillnessThresholdSeconds; }
    public void setStillnessThresholdSeconds(int stillnessThresholdSeconds) { this.stillnessThresholdSeconds = stillnessThresholdSeconds; }
    public int getDemoStillnessThresholdSeconds() { return demoStillnessThresholdSeconds; }
    public void setDemoStillnessThresholdSeconds(int demoStillnessThresholdSeconds) { this.demoStillnessThresholdSeconds = demoStillnessThresholdSeconds; }
    public double getSwayThreshold() { return swayThreshold; }
    public void setSwayThreshold(double swayThreshold) { this.swayThreshold = swayThreshold; }
    public String getQuietHoursStart() { return quietHoursStart; }
    public void setQuietHoursStart(String quietHoursStart) { this.quietHoursStart = quietHoursStart; }
    public String getQuietHoursEnd() { return quietHoursEnd; }
    public void setQuietHoursEnd(String quietHoursEnd) { this.quietHoursEnd = quietHoursEnd; }
    public int getOutOfBedBaselineSeconds() { return outOfBedBaselineSeconds; }
    public void setOutOfBedBaselineSeconds(int outOfBedBaselineSeconds) { this.outOfBedBaselineSeconds = outOfBedBaselineSeconds; }
    public int getDemoOutOfBedBaselineSeconds() { return demoOutOfBedBaselineSeconds; }
    public void setDemoOutOfBedBaselineSeconds(int demoOutOfBedBaselineSeconds) { this.demoOutOfBedBaselineSeconds = demoOutOfBedBaselineSeconds; }
    public int getFallImpactWindowSeconds() { return fallImpactWindowSeconds; }
    public void setFallImpactWindowSeconds(int fallImpactWindowSeconds) { this.fallImpactWindowSeconds = fallImpactWindowSeconds; }
    public int getFallLowPostureSeconds() { return fallLowPostureSeconds; }
    public void setFallLowPostureSeconds(int fallLowPostureSeconds) { this.fallLowPostureSeconds = fallLowPostureSeconds; }
    public Twilio getTwilio() { return twilio; }
    public void setTwilio(Twilio twilio) { this.twilio = twilio; }
}
