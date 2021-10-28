package io.platir.engine.setting;

import io.platir.engine.AccountSetting;

class AccountSettingCore implements AccountSetting {

    private final MaxNumberCheckerCore maxStrategyCount = new MaxNumberCheckerCore(1);
    private final EveryTimeCheckerCore settlementTime;

    AccountSettingCore() {
        settlementTime = new EveryTimeCheckerCore();
    }

    AccountSettingCore(AccountSetting accountSetting) {
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
