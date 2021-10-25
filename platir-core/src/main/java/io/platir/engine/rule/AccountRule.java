package io.platir.engine.rule;

import java.time.DayOfWeek;

public class AccountRule {

    private final MaxNumberRule maxMarginBalanceRatio = new MaxNumberRule(1.0D);
    private final MaxNumberRule maxMargin = new MaxNumberRule(Double.MAX_VALUE);
    private final MaxNumberRule maxCommission = new MaxNumberRule(Double.MAX_VALUE);
    private final MaxNumberRule maxPositionLoss = new MaxNumberRule(Double.MAX_VALUE);
    private final MaxNumberRule maxStrategyCount = new MaxNumberRule(1);
    private final EveryTimeSetter settlementTime = new EveryTimeSetter();

    public AccountRule(AccountRule accountRule) {
        maxMarginBalanceRatio.set(accountRule.maxMarginBalanceRatio().get());
        maxMargin.set(accountRule.maxMargin().get());
        maxCommission.set(accountRule.maxCommission().get());
        maxPositionLoss.set(accountRule.maxPositionLoss().get());
        maxStrategyCount.set(accountRule.maxStrategyCount().get());
        settlementTime.every(accountRule.settlementTime().getEveryTimes());
        settlementTime.except(accountRule.settlementTime().getExceptDates());
        settlementTime.except(accountRule.settlementTime().getExceptDaysOfWeek().toArray(new DayOfWeek[0]));
    }

    public MaxNumberRule maxMarginBalanceRatio() {
        return maxMarginBalanceRatio;
    }

    public MaxNumberRule maxMargin() {
        return maxMargin;
    }

    public MaxNumberRule maxCommission() {
        return maxCommission;
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
