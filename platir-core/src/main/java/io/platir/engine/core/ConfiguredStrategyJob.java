package io.platir.engine.core;

import io.platir.commons.StrategyCore;
import io.platir.engine.timer.EngineTimer;
import io.platir.engine.timer.TimerJob;
import io.platir.user.UserStrategy;
import java.time.LocalDateTime;
import java.util.logging.Level;

public class ConfiguredStrategyJob implements TimerJob {

    private final StrategyCore strategy;
    private final PlatirEngineCore engine;

    public ConfiguredStrategyJob(StrategyCore strategy, PlatirEngineCore engine) {
        this.strategy = strategy;
        this.engine = engine;
    }

    @Override
    public void onTime(LocalDateTime datetime, EngineTimer timer) {
        tryConfiguredOpen(datetime, timer);
        tryConfiguredClose(datetime, timer);
        tryAlarm(datetime, timer);
    }

    private void tryConfiguredOpen(LocalDateTime datetime, EngineTimer timer) {
        var checker = strategy.getStrategySetting().configuredOpenTime();
        if (checker.check(datetime)) {
            findUserStrategyAndCall(userStrategy -> userStrategy.onConfiguredOpen(), timer);
        }
    }

    private void tryConfiguredClose(LocalDateTime datetime, EngineTimer timer) {
        var checker = strategy.getStrategySetting().configuredCloseTime();
        if (checker.check(datetime)) {
            findUserStrategyAndCall(userStrategy -> userStrategy.onConfiguredClose(), timer);
        }
    }

    private void tryAlarm(LocalDateTime datetime, EngineTimer timer) {
        var everyChecker = strategy.getStrategySetting().alarmEveryTime();
        var pointChecker = strategy.getStrategySetting().alarmPointTime();
        if (everyChecker.check(datetime) || pointChecker.check(datetime)) {
            findUserStrategyAndCall(userStrategy -> userStrategy.onAlarm(), timer);
        }
    }

    private void findUserStrategyAndCall(Callback callback, EngineTimer timer) {
        try {
            var userStrategy = engine.getUserStrategyManager().getLookup().findStrategy(strategy);
            doCallback(callback, userStrategy);
        } catch (NoSuchUserStrategyException exception) {
            timer.removeJob(this);
            PlatirEngineCore.logger().log(Level.SEVERE, "No such user strategy({0}). {1}", new Object[]{strategy.getStrategyId(), exception.getMessage()});
        }
    }

    private void doCallback(Callback callback, UserStrategy userStrategy) {
        try {
            callback.call(userStrategy);
        } catch (Throwable throwable) {
            PlatirEngineCore.logger().log(Level.SEVERE, "Load strategy({0}) throws exception. {1}", new Object[]{strategy.getStrategyId(), throwable.getMessage()});
        }
    }

    @FunctionalInterface
    private interface Callback {

        void call(UserStrategy userStrategy);
    }
}
