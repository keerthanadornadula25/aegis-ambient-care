package com.aegis.engine;

import com.aegis.config.AegisProperties;
import com.aegis.dto.SensorEventDto;
import org.springframework.stereotype.Component;

@Component
public class RuleEngine {

    private final FallRule fallRule;
    private final HiddenFallRule hiddenFallRule;
    private final StillnessRule stillnessRule;
    private final NightWanderRule nightWanderRule;

    public RuleEngine(AegisProperties properties) {
        this.fallRule = new FallRule(properties.getFallLowPostureSeconds());
        this.hiddenFallRule = new HiddenFallRule(properties.getFallImpactWindowSeconds());
        this.stillnessRule = new StillnessRule(properties.getEffectiveStillnessSeconds(), properties.getSwayThreshold());
        this.nightWanderRule = new NightWanderRule(
                properties.getQuietHoursStart(),
                properties.getQuietHoursEnd(),
                properties.getEffectiveOutOfBedSeconds()
        );
    }

    public FallRule getFallRule() { return fallRule; }
    public HiddenFallRule getHiddenFallRule() { return hiddenFallRule; }
    public StillnessRule getStillnessRule() { return stillnessRule; }
    public NightWanderRule getNightWanderRule() { return nightWanderRule; }

    public RuleDecision evaluate(SensorEventDto event) {
        if (event == null) {
            return RuleDecision.noTrigger();
        }

        // 1. Evaluate Night Wander (Exterior Door Exit takes highest priority)
        RuleDecision nightDecision = nightWanderRule.evaluate(event);
        if (nightDecision.isTriggered()) {
            return nightDecision;
        }

        // 2. Evaluate Hidden Fall
        RuleDecision hiddenFallDecision = hiddenFallRule.evaluate(event);
        if (hiddenFallDecision.isTriggered()) {
            return hiddenFallDecision;
        }

        // 3. Evaluate Direct Fall
        RuleDecision fallDecision = fallRule.evaluate(event);
        if (fallDecision.isTriggered()) {
            return fallDecision;
        }

        // 4. Evaluate Stillness
        RuleDecision stillnessDecision = stillnessRule.evaluate(event);
        if (stillnessDecision.isTriggered()) {
            return stillnessDecision;
        }

        return RuleDecision.noTrigger();
    }

    public void reset() {
        fallRule.reset();
        hiddenFallRule.clear();
        nightWanderRule.clear();
    }
}
