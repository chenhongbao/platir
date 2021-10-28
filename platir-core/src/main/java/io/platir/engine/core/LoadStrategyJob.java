package io.platir.engine.core;

import io.platir.commons.StrategyCore;
import io.platir.engine.timer.EngineTimer;
import io.platir.engine.timer.TimerJob;
import java.time.LocalDateTime;
import java.util.logging.Level;

public class LoadStrategyJob implements TimerJob {

    private final StrategyCore strategy;
    private final PlatirEngineCore engine;

    public LoadStrategyJob(StrategyCore strategy, PlatirEngineCore engine) {
        this.strategy = strategy;
        this.engine = engine;
    }

    @Override
    public void onTime(LocalDateTime datetime, EngineTimer timer) {
        if (datetime.compareTo(strategy.getStrategySetting().getLoadDatetime()) < 0) {
            return;
        }
        try {
            callbackOnload(timer);
        } catch (Throwable throwable) {
            PlatirEngineCore.logger().log(Level.SEVERE, "Load strategy({0}) throws exception. {1}", new Object[]{strategy.getStrategyId(), throwable.getMessage()});
        } finally {
            timer.removeJob(this);
        }

    }

    private void callbackOnload(EngineTimer timer) {
        var userStrategyManager = engine.getUserStrategyManager();
        try {
            userStrategyManager.getLookup().findStrategy(strategy)
                    .onLoad(new UserSession((StrategyCore) strategy, engine.getTradingAdapter(), engine.getMarketDataAdapter(), userStrategyManager.getLoggingManager().getLoggingHandler(strategy)));
        } catch (NoSuchUserStrategyException exception) {
            PlatirEngineCore.logger().log(Level.SEVERE, "No user strategy found for strategy {0}. {1}", new Object[]{strategy.getStrategyId(), exception.getMessage()});
        }
    }
}
