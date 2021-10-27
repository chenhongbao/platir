package io.platir.engine.rule;

import io.platir.LoggingListener;

public class GlobalSetting {

    private Boolean marketDataParallel = true;
    private LoggingListener loggingListener;
    private final EveryTimeSetter reinitTime = new EveryTimeSetter();

    public GlobalSetting() {
    }

    public EveryTimeSetter reinitTime() {
        return reinitTime;
    }

    public void setMarketDataParallel(Boolean parallel) {
        if (parallel != null) {
            marketDataParallel = parallel;
        }
    }

    public boolean isMarketDataParallel() {
        return marketDataParallel;
    }

    public LoggingListener getLoggingListener() {
        return loggingListener;
    }

    public void setLoggingListener(LoggingListener loggingListener) {
        this.loggingListener = loggingListener;
    }

}
