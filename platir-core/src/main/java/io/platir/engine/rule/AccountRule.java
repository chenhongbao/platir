package io.platir.engine.rule;

public class AccountRule {

    private final MaxNumberRule maxStrategyCount = new MaxNumberRule(1);
    private final EveryTimeSetter settlementTime;

    public AccountRule() {
        settlementTime = new EveryTimeSetter();
    }

    public AccountRule(AccountRule accountRule) {
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
