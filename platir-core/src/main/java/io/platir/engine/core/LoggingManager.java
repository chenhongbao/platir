package io.platir.engine.core;

import io.platir.commons.StrategyCore;
import java.util.logging.Handler;

class LoggingManager {

    Handler getLoggingHandler(StrategyCore strategy) {
        return new LoggingDispatcher(strategy);
    }

}
