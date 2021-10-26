package io.platir.engine.rule;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

public class StrategyRule {

    private LocalDateTime loadTime;
    private final EveryTimeSetter configuredOpenTime;
    private final EveryTimeSetter configuredCloseTime;
    private final EveryTimeSetter alarmEveryTime;
    private final PointTimeSetter alarmPointTime;
    private final MaxNumberRule maxTransactionOnline = new MaxNumberRule(Integer.MAX_VALUE);
    private final MaxNumberRule maxOrderQuantity = new MaxNumberRule(Integer.MAX_VALUE);
    private final Map<String, String> parameters = new HashMap<>();
    
    public StrategyRule() {
        loadTime = null;
        configuredOpenTime = new EveryTimeSetter();
        configuredCloseTime = new EveryTimeSetter();
        alarmEveryTime = new EveryTimeSetter();
        alarmPointTime = new PointTimeSetter();
    }

    public StrategyRule(StrategyRule strategyRule) {
        loadTime = strategyRule.getLoadTime();
        configuredOpenTime = new EveryTimeSetter(strategyRule.configuredOpenTime());
        configuredCloseTime = new EveryTimeSetter(strategyRule.configuredCloseTime());
        alarmEveryTime = new EveryTimeSetter(strategyRule.alarmEveryTime());
        alarmPointTime = new PointTimeSetter(strategyRule.alarmPointTime());
    }

    public LocalDateTime getLoadTime() {
        return loadTime;
    }

    public void setLoadTime(LocalDateTime loadTime) {
        this.loadTime = loadTime;
    }

    public EveryTimeSetter configuredOpenTime() {
        return configuredOpenTime;
    }

    public EveryTimeSetter configuredCloseTime() {
        return configuredCloseTime;
    }

    public EveryTimeSetter alarmEveryTime() {
        return alarmEveryTime;
    }

    public PointTimeSetter alarmPointTime() {
        return alarmPointTime;
    }

    public MaxNumberRule maxOnlineTransactionCount() {
        return maxTransactionOnline;
    }

    public MaxNumberRule maxOrderQuantity() {
        return maxOrderQuantity;
    }

    public Map<String, String> parameters() {
        return parameters;
    }
}
