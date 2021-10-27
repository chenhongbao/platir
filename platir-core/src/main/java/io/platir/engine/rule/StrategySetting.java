package io.platir.engine.rule;

import io.platir.LoggingListener;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

public class StrategySetting {

    private LocalDateTime loadTime;
    private LoggingListener loggingListener;
    private final EveryTimeSetter configuredOpenTime;
    private final EveryTimeSetter configuredCloseTime;
    private final EveryTimeSetter alarmEveryTime;
    private final PointTimeSetter alarmPointTime;
    private final Map<String, String> parameters = new HashMap<>();

    public StrategySetting() {
        loadTime = null;
        configuredOpenTime = new EveryTimeSetter();
        configuredCloseTime = new EveryTimeSetter();
        alarmEveryTime = new EveryTimeSetter();
        alarmPointTime = new PointTimeSetter();
    }

    public StrategySetting(StrategySetting strategyRule) {
        loadTime = strategyRule.getLoadTime();
        loggingListener = strategyRule.getLoggingListener();
        configuredOpenTime = new EveryTimeSetter(strategyRule.configuredOpenTime());
        configuredCloseTime = new EveryTimeSetter(strategyRule.configuredCloseTime());
        alarmEveryTime = new EveryTimeSetter(strategyRule.alarmEveryTime());
        alarmPointTime = new PointTimeSetter(strategyRule.alarmPointTime());
    }

    public void setLoggingListener(LoggingListener logginglistener) {
        this.loggingListener = logginglistener;
    }

    public LoggingListener getLoggingListener() {
        return loggingListener;
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

    public Map<String, String> parameters() {
        return parameters;
    }
}
