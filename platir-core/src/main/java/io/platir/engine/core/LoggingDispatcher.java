package io.platir.engine.core;

import io.platir.LoggingListener;
import io.platir.commons.StrategyCore;
import io.platir.utils.Utils;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.SimpleFormatter;

class LoggingDispatcher extends Handler {

    private final FileHandler file;
    private final Set<LoggingListener> listeners = new ConcurrentSkipListSet<>();

    LoggingDispatcher(Set<LoggingListener> listeners) {
        this.listeners.addAll(listeners);
        file = getDefaultFileHandler(null);
    }

    LoggingDispatcher(StrategyCore strategy) {
        this.listeners.addAll(strategy.getStrategySetting().getLoggingListeners());
        file = getDefaultFileHandler(strategy);
    }

    @Override
    public void publish(LogRecord record) {
        listeners.parallelStream().forEach(listener -> listener.onLog(record));
        if (file != null) {
            file.publish(record);
            file.flush();
        }
    }

    @Override
    public void flush() {
    }

    @Override
    public void close() throws SecurityException {
        listeners.clear();
    }

    private FileHandler getDefaultFileHandler(StrategyCore strategy) {
        try {
            Path logPath;
            if (strategy != null) {
                logPath = Paths.get(Commons.loggingDirectory().toString(), strategy.getAccount().getUser().getUserId(), strategy.getAccount().getAccountId(), strategy.getStrategyId() + ".log");
            } else {
                logPath = Paths.get(Commons.loggingDirectory().toString(), "default.log");
            }
            var handler = new FileHandler(Utils.file(logPath).getAbsolutePath(), true);
            handler.setFormatter(new SimpleFormatter());
            return handler;
        } catch (IOException | SecurityException ex) {
            var strategyId = "";
            if (strategy != null) {
                strategyId = strategy.getStrategyId();
            }
            PlatirEngineCore.logger().log(Level.SEVERE, "Can''t create logging file handler for strategy({0}). {1}", new Object[]{strategyId, ex.getMessage()});
            return null;
        }
    }

}
