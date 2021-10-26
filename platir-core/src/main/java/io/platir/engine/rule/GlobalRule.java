package io.platir.engine.rule;

public class GlobalRule {

    private final EveryTimeSetter reloadTime = new EveryTimeSetter();

    public GlobalRule() {
    }

    public EveryTimeSetter reloadTime() {
        return reloadTime;
    }
}
