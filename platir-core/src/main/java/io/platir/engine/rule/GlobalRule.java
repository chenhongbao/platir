package io.platir.engine.rule;

public class GlobalRule {

    private final EveryTimeSetter reinitTime = new EveryTimeSetter();

    public GlobalRule() {
    }

    public EveryTimeSetter reinitTime() {
        return reinitTime;
    }
}
