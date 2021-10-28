package io.platir.engine.timer;

import java.time.LocalDateTime;

public interface TimerJob {
    void onTime(LocalDateTime datetime, EngineTimer timer);
}
