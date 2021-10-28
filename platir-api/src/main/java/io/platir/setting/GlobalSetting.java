package io.platir.setting;

import io.platir.LoggingListener;
import java.util.Set;

public interface GlobalSetting {

    boolean isMarketDataParallel();
    
    boolean isInitialDefered();

    Set<LoggingListener> getLoggingListeners();

    EveryTimeChecker reinitTime();

    void setMarketDataParallel(boolean parallel);

    void addLoggingListener(LoggingListener loggingListener);
    
    void setInitialDefered(boolean defered);

}
