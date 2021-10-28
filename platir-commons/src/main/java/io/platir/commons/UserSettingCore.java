package io.platir.commons;

import io.platir.setting.UserSetting;

public class UserSettingCore implements UserSetting {

    private final MaxNumberCheckerCore maxAccountCount = new MaxNumberCheckerCore(1);
    private final MaxNumberCheckerCore maxInitialBalance = new MaxNumberCheckerCore(Double.MAX_VALUE);

    public UserSettingCore() {
    }

    public UserSettingCore(UserSettingCore userSetting) {
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
