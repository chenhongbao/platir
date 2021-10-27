package io.platir;

import java.util.logging.LogRecord;

public interface LoggingListener {

    void onLog(LogRecord logRecord);
}
