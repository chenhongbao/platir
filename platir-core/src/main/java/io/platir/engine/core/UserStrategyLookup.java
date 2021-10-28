package io.platir.engine.core;

import io.platir.Strategy;
import io.platir.commons.StrategyCore;
import io.platir.user.UserStrategy;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

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

    void reload(Set<StrategyCore> reloadStrategies) {
        reloadStrategies.forEach(strategy -> {
            if (strategy.getState().equals(Strategy.REMOVED)) {
                if (userStrategies.remove(strategy) == null) {
                    PlatirEngineCore.logger().log(Level.WARNING, "Strategy({0}) has no user strategy.", strategy.getStrategyId());
                }
            }
        });
    }
}
