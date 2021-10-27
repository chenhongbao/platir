package io.platir.engine.rule;

public class AccountSetting {

    private final MaxNumberRule maxStrategyCount = new MaxNumberRule(1);
    private final EveryTimeSetter settlementTime;

    public AccountSetting() {
        settlementTime = new EveryTimeSetter();
    }

    public AccountSetting(AccountSetting accountRule) {
        maxStrategyCount.set(accountRule.maxStrategyCount().get());
        settlementTime = new EveryTimeSetter(accountRule.settlementTime());
    }

    public MaxNumberRule maxStrategyCount() {
        return maxStrategyCount;
    }

    public EveryTimeSetter settlementTime() {
        return settlementTime;
    }
}
