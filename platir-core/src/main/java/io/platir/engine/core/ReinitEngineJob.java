package io.platir.engine.core;

import io.platir.engine.InitializeEngineException;
import io.platir.engine.timer.EngineTimer;
import io.platir.engine.timer.TimerJob;
import java.time.LocalDateTime;
import java.util.logging.Level;

public class ReinitEngineJob implements TimerJob {

    private final PlatirEngineCore engine;

    public ReinitEngineJob(PlatirEngineCore engine) {
        this.engine = engine;
    }

    @Override
    public void onTime(LocalDateTime datetime, EngineTimer timer) {
        try {
            var setting = engine.getGlobalSetting();
            if (setting.reinitTime().check(datetime)) {
                reinitEngine();
            }
        } catch (Throwable throwable) {
            PlatirEngineCore.logger().log(Level.SEVERE, "Re-initialize engine throws exception. {0}", throwable.getMessage());
        }
    }

    private void reinitEngine() {
        try {
            engine.initializeNow();
        } catch (InitializeEngineException exception) {
            PlatirEngineCore.logger().log(Level.SEVERE, "Re-initialize engine throws exception. {0}", exception.getMessage());
        }
    }

}
