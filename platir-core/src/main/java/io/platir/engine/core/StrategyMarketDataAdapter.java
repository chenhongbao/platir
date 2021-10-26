package io.platir.engine.core;

import io.platir.Strategy;
import io.platir.broker.Bar;
import io.platir.broker.MarketDataSnapshot;
import io.platir.util.Utils;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

class StrategyMarketDataAdapter {

    private final UserStrategyLookup userStrategyLookup;
    private final Map<String, StrategyMarketDataQueue> strategies = new ConcurrentHashMap<>();

    StrategyMarketDataAdapter(UserStrategyLookup userStrategyLookup) {
        this.userStrategyLookup = userStrategyLookup;
    }

    void remove(String strategyId) {
        strategies.remove(strategyId);
    }

    void add(Strategy strategy) {
        var queue = new StrategyMarketDataQueue(strategy.getStrategyId(), this, userStrategyLookup);
        Utils.threads().submit(queue);
        strategies.put(strategy.getStrategyId(), queue);
    }

    void broadcast(MarketDataSnapshot marketDataSnapshot) {
        strategies.values().parallelStream().forEach(queue -> queue.push(marketDataSnapshot));
    }

    void broadcast(Bar bar) {
        strategies.values().parallelStream().forEach(queue -> queue.push(bar));
    }

}
