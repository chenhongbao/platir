package io.platir.engine.core;

import io.platir.user.UserStrategy;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

class UserStrategyLookup {

    private final Map<StrategyCore, UserStrategy> userStrategies = new ConcurrentHashMap<>();

    UserStrategy findStrategy(StrategyCore strategy) throws NoSuchUserStrategyException {
        if (!userStrategies.containsKey(strategy)) {
            throw new NoSuchUserStrategyException("No user strategy found for " + strategy.getStrategyId() + ".");
        }
        return userStrategies.get(strategy);
    }

    void putStrategy(StrategyCore strategy, UserStrategy userStrategy) {
        userStrategies.put(strategy, userStrategy);
    }

    UserStrategy removeStrategy(StrategyCore strategy) {
        return userStrategies.remove(strategy);
    }
}
