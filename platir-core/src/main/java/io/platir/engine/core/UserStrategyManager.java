package io.platir.engine.core;

import io.platir.commons.StrategyCore;
import io.platir.commons.UserCore;
import io.platir.engine.AddStrategyException;
import io.platir.user.UserStrategy;
import java.util.HashSet;
import java.util.Set;

class UserStrategyManager {

    private final UserStrategyLookup lookup = new UserStrategyLookup();
    private final LoggingManager logging = new LoggingManager();

    void addUserStrategy(StrategyCore strategy, UserStrategy userStrategy) throws AddStrategyException {
        lookup.putStrategy(strategy, userStrategy);
    }

    UserStrategy removeUserStrategy(StrategyCore removed) {
        return lookup.removeStrategy(removed);
    }

    UserStrategyLookup getLookup() {
        return lookup;
    }
    
    LoggingManager getLoggingManager() {
        return logging;
    }

    void reload(Set<UserCore> users) {
        Set<StrategyCore> strategies = new HashSet<>();
        users.forEach(user -> {
            user.accounts().values().forEach(account -> {
                strategies.addAll(account.strategies().values());
            });
        });
        lookup.reload(strategies);
    }

}
