package io.platir.commons;

import io.platir.setting.AccountSetting;

public class AccountSettingCore implements AccountSetting {

    private final MaxNumberCheckerCore maxStrategyCount = new MaxNumberCheckerCore(1);
    private final EveryTimeCheckerCore settlementTime;

    public AccountSettingCore() {
        settlementTime = new EveryTimeCheckerCore();
    }

    public AccountSettingCore(AccountSettingCore accountSetting) {
        maxStrategyCount.set(accountSetting.maxStrategyCount().get());
        settlementTime = new EveryTimeCheckerCore(accountSetting.settlementTime());
    }

    @Override
    public MaxNumberCheckerCore maxStrategyCount() {
        return maxStrategyCount;
    }

    @Override
    public EveryTimeCheckerCore settlementTime() {
        return settlementTime;
    }
}
