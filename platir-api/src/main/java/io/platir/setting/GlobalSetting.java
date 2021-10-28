package io.platir.setting;

import io.platir.LoggingListener;
import java.util.Set;

public interface GlobalSetting {

    void addLoggingListener(LoggingListener loggingListener);

    Set<LoggingListener> getLoggingListeners();

    boolean isMarketDataParallel();

    EveryTimeChecker reinitTime();

    void setMarketDataParallel(Boolean parallel);

}
