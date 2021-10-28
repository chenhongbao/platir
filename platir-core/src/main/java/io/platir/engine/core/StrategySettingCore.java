package io.platir.engine.core;

import io.platir.engine.StrategySetting;
import io.platir.LoggingListener;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;

class StrategySettingCore implements StrategySetting {

    private LocalDateTime loadTime;
    private final Set<LoggingListener> loggingListeners = new ConcurrentSkipListSet<>();
    private final EveryTimeCheckerCore configuredOpenTime;
    private final EveryTimeCheckerCore configuredCloseTime;
    private final EveryTimeCheckerCore alarmEveryTime;
    private final PointTimeCheckerCore alarmPointTime;
    private final Map<String, String> parameters = new HashMap<>();

    StrategySettingCore() {
        loadTime = null;
        configuredOpenTime = new EveryTimeCheckerCore();
        configuredCloseTime = new EveryTimeCheckerCore();
        alarmEveryTime = new EveryTimeCheckerCore();
        alarmPointTime = new PointTimeCheckerCore();
    }

    StrategySettingCore(StrategySettingCore strategySetting) {
        loadTime = strategySetting.getLoadTime();
        configuredOpenTime = new EveryTimeCheckerCore(strategySetting.configuredOpenTime());
        configuredCloseTime = new EveryTimeCheckerCore(strategySetting.configuredCloseTime());
        alarmEveryTime = new EveryTimeCheckerCore(strategySetting.alarmEveryTime());
        alarmPointTime = new PointTimeCheckerCore(strategySetting.alarmPointTime());
        loggingListeners.addAll(strategySetting.getLoggingListeners());
    }

    @Override
    public void addLoggingListener(LoggingListener logginglistener) {
        loggingListeners.add(logginglistener);
    }

    @Override
    public Set<LoggingListener> getLoggingListeners() {
        return new HashSet<>(loggingListeners);
    }

    @Override
    public LocalDateTime getLoadTime() {
        return loadTime;
    }

    @Override
    public void setLoadTime(LocalDateTime loadTime) {
        this.loadTime = loadTime;
    }

    @Override
    public EveryTimeCheckerCore configuredOpenTime() {
        return configuredOpenTime;
    }

    @Override
    public EveryTimeCheckerCore configuredCloseTime() {
        return configuredCloseTime;
    }

    @Override
    public EveryTimeCheckerCore alarmEveryTime() {
        return alarmEveryTime;
    }

    @Override
    public PointTimeCheckerCore alarmPointTime() {
        return alarmPointTime;
    }

    @Override
    public Map<String, String> parameters() {
        return parameters;
    }
}
