package io.platir.commons;

import io.platir.setting.GlobalSetting;
import io.platir.LoggingListener;
import io.platir.setting.EveryTimeChecker;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;

public class GlobalSettingCore implements GlobalSetting {

    private Boolean initialDefered = false;
    private Boolean marketDataParallel = true;
    private final Set<LoggingListener> loggingListeners = new ConcurrentSkipListSet<>();
    private final EveryTimeCheckerCore reinitTime;
    private final EveryTimeCheckerCore clearTime;

    public GlobalSettingCore() {
        reinitTime = new EveryTimeCheckerCore();
        clearTime = new EveryTimeCheckerCore();
    }

    public GlobalSettingCore(GlobalSettingCore globalSetting) {
        marketDataParallel = globalSetting.isMarketDataParallel();
        reinitTime = new EveryTimeCheckerCore(globalSetting.reinitTime());
        clearTime = new EveryTimeCheckerCore(globalSetting.clearTime());
        loggingListeners.addAll(globalSetting.getLoggingListeners());
    }

    @Override
    public EveryTimeCheckerCore reinitTime() {
        return reinitTime;
    }

    @Override
    public void setMarketDataParallel(boolean parallel) {
        marketDataParallel = parallel;
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

    @Override
    public boolean isInitialDefered() {
        return initialDefered;
    }

    @Override
    public void setInitialDefered(boolean defered) {
        initialDefered = defered;
    }

    @Override
    public EveryTimeChecker clearTime() {
        return clearTime;
    }

}
