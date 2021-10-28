package io.platir.engine.timer;

public abstract class EngineTimer {
    public static EngineTimer newTimer() {
        return new EngineTimerCore();
    }
    
    public abstract void addJob(TimerJob job);
    
    public abstract void removeJob(TimerJob job);
}
