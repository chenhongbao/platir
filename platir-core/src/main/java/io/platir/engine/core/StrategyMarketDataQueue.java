package io.platir.engine.core;

import io.platir.broker.Bar;
import io.platir.broker.MarketDataSnapshot;
import io.platir.user.UserStrategy;
import io.platir.util.Utils;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

class StrategyMarketDataQueue implements Runnable {

    private final String strategyId;
    private final UserStrategyLookup userStrategyLookup;
    private final StrategyMarketDataAdapter strategyMarketDataAdapter;
    private final BlockingQueue<Object> queue = new LinkedBlockingQueue<>();

    StrategyMarketDataQueue(String strategyId, StrategyMarketDataAdapter strategyMarketDataAdapter, UserStrategyLookup userStrategyLookup) {
        this.strategyId = strategyId;
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
                Utils.logger().log(Level.SEVERE, "Strategy({0}) market data callback throws exception. {1}", new Object[]{strategyId, exception.getMessage()});
            }
        }
        /* Remove itself out of adapter. */
        strategyMarketDataAdapter.remove(strategyId);
        queue.clear();
    }

    void push(Object marketData) {
        if (!queue.offer(marketData)) {
            Utils.logger().log(Level.SEVERE, "Strategy({0}) market data queue is full.", strategyId);
        }
    }

    void pushSync(Object marketData) {
        try {
            callback(marketData);
        } catch (NoSuchUserStrategyException exception) {
            strategyMarketDataAdapter.remove(strategyId);
        } catch (Throwable exception) {
            Utils.logger().log(Level.SEVERE, "Strategy({0}) market data callback throws exception. {1}", new Object[]{strategyId, exception.getMessage()});
        }
    }

    private void callback(Object marketData) throws NoSuchUserStrategyException {
        UserStrategy userStrategy = userStrategyLookup.find(strategyId);
        if (marketData instanceof Bar) {
            userStrategy.onBar((Bar) marketData);
        } else if (marketData instanceof MarketDataSnapshot) {
            userStrategy.onMarketDataSnapshot((MarketDataSnapshot) marketData);
        } else {
            Utils.logger().log(Level.SEVERE, "Strategy({0}) receives wrong market data. ", strategyId);
        }
    }
}
