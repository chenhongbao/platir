package io.platir.setting;

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

    LocalDateTime getLoadDatetime();

    /**
     * Set strategy loading datetime.
     * <p>
     * If the specified datetime is past and the strategy is not loaded, load
     * the strategy once and only once. Upon loading strategy,
     * {@linkplain io.platir.user.UserStrategy#onLoad UserStrategy.onLoad} is called.
     *
     * @param datetime datetime when the specified strategy is loaded
     */
    void setLoadDatetime(LocalDateTime datetime);

    Set<LoggingListener> getLoggingListeners();

    Map<String, String> parameters();

}
