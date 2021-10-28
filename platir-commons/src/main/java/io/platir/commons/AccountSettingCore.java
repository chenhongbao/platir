package io.platir.commons;

import io.platir.setting.AccountSetting;

public class AccountSettingCore implements AccountSetting {

    private final MaxNumberCheckerCore maxStrategyCount = new MaxNumberCheckerCore(1);

    public AccountSettingCore() {
    }

    public AccountSettingCore(AccountSettingCore accountSetting) {
        maxStrategyCount.set(accountSetting.maxStrategyCount().get());
    }

    @Override
    public MaxNumberCheckerCore maxStrategyCount() {
        return maxStrategyCount;
    }
}
