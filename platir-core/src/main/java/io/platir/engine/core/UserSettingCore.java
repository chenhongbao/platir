package io.platir.engine.core;

import io.platir.engine.UserSetting;

final class UserSettingCore implements UserSetting {

    private final MaxNumberCheckerCore maxAccountCount = new MaxNumberCheckerCore(1);
    private final MaxNumberCheckerCore maxInitialBalance = new MaxNumberCheckerCore(Double.MAX_VALUE);

    UserSettingCore() {
    }

    UserSettingCore(UserSettingCore userSetting) {
        maxAccountCount.set(userSetting.maxAccountCount().get());
        maxInitialBalance.set(userSetting.maxInitialBalance().get());
    }

    @Override
    public MaxNumberCheckerCore maxAccountCount() {
        return maxAccountCount;
    }

    @Override
    public MaxNumberCheckerCore maxInitialBalance() {
        return maxInitialBalance;
    }

}
