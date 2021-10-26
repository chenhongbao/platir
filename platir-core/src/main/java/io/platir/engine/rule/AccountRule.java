package io.platir.engine.rule;

public class AccountRule {

    private final MaxNumberRule maxMarginBalanceRatio = new MaxNumberRule(1.0D);
    private final MaxNumberRule maxMargin = new MaxNumberRule(Double.MAX_VALUE);
    private final MaxNumberRule maxPositionLoss = new MaxNumberRule(Double.MAX_VALUE);
    private final MaxNumberRule maxStrategyCount = new MaxNumberRule(1);
    private final EveryTimeSetter settlementTime;

    public AccountRule() {
        settlementTime = new EveryTimeSetter();
    }

    public AccountRule(AccountRule accountRule) {
        maxMarginBalanceRatio.set(accountRule.maxMarginBalanceRatio().get());
        maxMargin.set(accountRule.maxMargin().get());
        maxPositionLoss.set(accountRule.maxPositionLoss().get());
        maxStrategyCount.set(accountRule.maxStrategyCount().get());
        settlementTime = new EveryTimeSetter(accountRule.settlementTime());
    }

    public MaxNumberRule maxMarginBalanceRatio() {
        return maxMarginBalanceRatio;
    }

    public MaxNumberRule maxMargin() {
        return maxMargin;
    }

    public MaxNumberRule maxPositionLoss() {
        return maxPositionLoss;
    }

    public MaxNumberRule maxStrategyCount() {
        return maxStrategyCount;
    }

    public EveryTimeSetter settlementTime() {
        return settlementTime;
    }
}
