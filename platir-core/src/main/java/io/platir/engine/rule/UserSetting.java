package io.platir.engine.rule;

public final class UserSetting {

    private final MaxNumberRule maxAccountCount = new MaxNumberRule(1);
    private final MaxNumberRule maxInitialBalance = new MaxNumberRule(Double.MAX_VALUE);

    public UserSetting() {
    }

    public UserSetting(UserSetting userRule) {
        maxAccountCount.set(userRule.maxAccountCount().get());
        maxInitialBalance.set(userRule.maxInitialBalance().get());
    }

    public MaxNumberRule maxAccountCount() {
        return maxAccountCount;
    }

    public MaxNumberRule maxInitialBalance() {
        return maxInitialBalance;
    }

}
