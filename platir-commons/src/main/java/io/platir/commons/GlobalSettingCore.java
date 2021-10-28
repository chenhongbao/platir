package io.platir.commons;

import io.platir.setting.GlobalSetting;
import io.platir.LoggingListener;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;

public class GlobalSettingCore implements GlobalSetting {

    private Boolean marketDataParallel = true;
    private final Set<LoggingListener> loggingListeners = new ConcurrentSkipListSet<>();
    private final EveryTimeCheckerCore reinitTime;

    public GlobalSettingCore() {
        reinitTime = new EveryTimeCheckerCore();
    }

    public GlobalSettingCore(GlobalSettingCore globalSetting) {
        marketDataParallel = globalSetting.isMarketDataParallel();
        reinitTime = new EveryTimeCheckerCore(globalSetting.reinitTime());
        loggingListeners.addAll(globalSetting.getLoggingListeners());
    }

    @Override
    public EveryTimeCheckerCore reinitTime() {
        return reinitTime;
    }

    @Override
    public void setMarketDataParallel(Boolean parallel) {
        if (parallel != null) {
            marketDataParallel = parallel;
        }
    }

    @Override
    public boolean isMarketDataParallel() {
        return marketDataParallel;
    }

    @Override
    public Set<LoggingListener> getLoggingListeners() {
        return new HashSet<>(loggingListeners);
    }

    @Override
    public void addLoggingListener(LoggingListener loggingListener) {
        loggingListeners.add(loggingListener);
    }

}
