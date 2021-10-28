package io.platir.engine.core;

import io.platir.commons.StrategyCore;
import io.platir.engine.AddStrategyException;
import io.platir.user.UserStrategy;

class UserStrategyManager {

    private final UserStrategyLookup lookup = new UserStrategyLookup();
    private final LoggingManager logging = new LoggingManager();

    void addUserStrategy(StrategyCore strategy, UserStrategy userStrategy) throws AddStrategyException {
        lookup.putStrategy(strategy, userStrategy);
    }

    UserStrategy removeUserStrategy(StrategyCore removed) {
        logging.removeStrategy(removed);
        return lookup.removeStrategy(removed);
    }

    UserStrategyLookup getLookup() {
        return lookup;
    }
    
    LoggingManager getLoggingManager() {
        return logging;
    }

}
