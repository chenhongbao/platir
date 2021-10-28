package io.platir.engine.core;

import io.platir.commons.StrategyCore;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

class StrategyMarketDataAdapter {

    private final Boolean isParallel;
    private final UserStrategyLookup userStrategyLookup;
    private final Map<StrategyCore, StrategyMarketDataQueue> strategies = new ConcurrentHashMap<>();

    StrategyMarketDataAdapter(UserStrategyLookup userStrategyLookup, Boolean parallel) {
        this.isParallel = parallel;
        this.userStrategyLookup = userStrategyLookup;
    }

    void remove(StrategyCore strategy) {
        strategies.remove(strategy);
    }

    void add(StrategyCore strategy) {
        var queue = new StrategyMarketDataQueue(strategy, this, userStrategyLookup);
        if (isParallel) {
            PlatirEngineCore.threads().submit(queue);
        }
        strategies.put(strategy, queue);
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
