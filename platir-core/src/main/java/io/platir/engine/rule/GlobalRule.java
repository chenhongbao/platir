package io.platir.engine.rule;

public class GlobalRule {

    private Boolean marketDataParallel = true;
    private final EveryTimeSetter reinitTime = new EveryTimeSetter();

    public GlobalRule() {
    }

    public EveryTimeSetter reinitTime() {
        return reinitTime;
    }

    public void setMarketDataParallel(Boolean parallel) {
        if (parallel != null) {
            marketDataParallel = parallel;
        }
    }

    public boolean isMarketDataParallel() {
        return marketDataParallel;
    }
}
