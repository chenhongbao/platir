package io.platir.engine.core;

import io.platir.Strategy;
import io.platir.broker.Bar;
import io.platir.broker.MarketDataSnapshot;
import io.platir.util.Utils;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

class StrategyMarketDataAdapter {

    private final Boolean isParallel;
    private final UserStrategyLookup userStrategyLookup;
    private final Map<String, StrategyMarketDataQueue> strategies = new ConcurrentHashMap<>();

    StrategyMarketDataAdapter(UserStrategyLookup userStrategyLookup, Boolean parallel) {
        this.isParallel = parallel;
        this.userStrategyLookup = userStrategyLookup;
    }

    void remove(String strategyId) {
        strategies.remove(strategyId);
    }

    void add(Strategy strategy) {
        var queue = new StrategyMarketDataQueue(strategy.getStrategyId(), this, userStrategyLookup);
        if (isParallel) {
            Utils.threads().submit(queue);
        }
        strategies.put(strategy.getStrategyId(), queue);
    }

    void broadcast(Object marketData) {
        strategies.values().forEach(queue -> {
            if (isParallel) {
                queue.push(marketData);
            } else {
                queue.pushSync(marketData);
            }
        });
    }
}
