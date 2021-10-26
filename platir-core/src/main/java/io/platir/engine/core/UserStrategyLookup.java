package io.platir.engine.core;

import io.platir.user.UserStrategy;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

class UserStrategyLookup {

    private final Map<String, UserStrategy> userStrategies = new ConcurrentHashMap<>();

    UserStrategy find(String strategyId) throws NoSuchUserStrategyException {
        if (!userStrategies.containsKey(strategyId)) {
            throw new NoSuchUserStrategyException("No user strategy found for " + strategyId + ".");
        }
        return userStrategies.get(strategyId);
    }

}
