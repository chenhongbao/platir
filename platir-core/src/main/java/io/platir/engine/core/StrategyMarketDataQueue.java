package io.platir.engine.core;

import io.platir.commons.StrategyCore;
import io.platir.Strategy;
import io.platir.broker.Bar;
import io.platir.broker.MarketDataSnapshot;
import io.platir.user.UserStrategy;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

class StrategyMarketDataQueue implements Runnable {

    private final StrategyCore strategy;
    private final UserStrategyLookup userStrategyLookup;
    private final StrategyMarketDataAdapter strategyMarketDataAdapter;
    private final BlockingQueue<Object> queue = new LinkedBlockingQueue<>();

    StrategyMarketDataQueue(StrategyCore strategy, StrategyMarketDataAdapter strategyMarketDataAdapter, UserStrategyLookup userStrategyLookup) {
        this.strategy = strategy;
        this.userStrategyLookup = userStrategyLookup;
        this.strategyMarketDataAdapter = strategyMarketDataAdapter;
    }

    @Override
    public void run() {
        while (true) {
            try {
                callback(queue.poll(24, TimeUnit.DAYS));
            } catch (NoSuchUserStrategyException exception) {
                /* User strategy not found, so it is unavailable. */
                break;
            } catch (Throwable exception) {
                PlatirEngineCore.logger().log(Level.SEVERE, "Strategy({0}) market data callback throws exception. {1}", new Object[]{strategy.getStrategyId(), exception.getMessage()});
            }
        }
        /* Remove itself out of adapter. */
        strategyMarketDataAdapter.remove(strategy);
        queue.clear();
    }

    void push(Object marketData) {
        if (!queue.offer(marketData)) {
            PlatirEngineCore.logger().log(Level.SEVERE, "Strategy({0}) market data queue is full.", strategy.getStrategyId());
        }
    }

    void pushSync(Object marketData) {
        try {
            callback(marketData);
        } catch (NoSuchUserStrategyException exception) {
            strategyMarketDataAdapter.remove(strategy);
        } catch (Throwable exception) {
            PlatirEngineCore.logger().log(Level.SEVERE, "Strategy({0}) market data callback throws exception. {1}", new Object[]{strategy.getStrategyId(), exception.getMessage()});
        }
    }

    private void callback(Object marketData) throws NoSuchUserStrategyException {
        synchronized (strategy.syncObject()) {
            if (!strategy.getState().equals(Strategy.NORMAL)) {
                return;
            }
        }
        UserStrategy userStrategy = userStrategyLookup.findStrategy(strategy);
        if (marketData instanceof Bar) {
            userStrategy.onBar((Bar) marketData);
        } else if (marketData instanceof MarketDataSnapshot) {
            userStrategy.onMarketDataSnapshot((MarketDataSnapshot) marketData);
        } else {
            PlatirEngineCore.logger().log(Level.SEVERE, "Strategy({0}) receives wrong market data. ", strategy.getStrategyId());
        }
    }
}
