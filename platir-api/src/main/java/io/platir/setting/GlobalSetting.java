package io.platir.setting;

import io.platir.LoggingListener;
import java.util.Set;

public interface GlobalSetting {

    boolean isMarketDataParallel();

    Set<LoggingListener> getLoggingListeners();

    EveryTimeChecker reinitTime();

    void setMarketDataParallel(Boolean parallel);

    void addLoggingListener(LoggingListener loggingListener);

}
