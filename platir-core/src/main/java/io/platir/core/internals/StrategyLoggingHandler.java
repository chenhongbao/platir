package io.platir.core.internals;

import java.util.LinkedList;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.LogRecord;

/**
 *
 * @author Chen Hongbao
 */
class StrategyLoggingHandler extends Handler {

    private final List<LogRecord> records = new LinkedList<>();

    StrategyLoggingHandler() {
    }

    @Override
    public void publish(LogRecord record) {
        synchronized (records) {
            records.add(record);
        }
    }

    @Override
    public void flush() {
    }

    @Override
    public void close() throws SecurityException {
    }

    List<LogRecord> getLogRecords() {
        synchronized (records) {
            var r = new LinkedList<LogRecord>();
            r.addAll(records);
            return r;
        }
    }

    void clear() {
        synchronized (records) {
            records.clear();
        }
    }

}
