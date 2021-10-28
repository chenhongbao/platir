package io.platir.engine;

import io.platir.LoggingListener;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;

public interface StrategySetting {

    void addLoggingListener(LoggingListener logginglistener);

    EveryTimeChecker alarmEveryTime();

    PointTimeChecker alarmPointTime();

    EveryTimeChecker configuredCloseTime();

    EveryTimeChecker configuredOpenTime();

    LocalDateTime getLoadTime();

    void setLoadTime(LocalDateTime loadTime);

    Set<LoggingListener> getLoggingListeners();

    Map<String, String> parameters();

}
