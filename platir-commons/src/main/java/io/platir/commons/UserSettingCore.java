package io.platir.commons;

import io.platir.setting.EveryTimeChecker;
import io.platir.setting.UserSetting;

public class UserSettingCore implements UserSetting {

    private final EveryTimeCheckerCore settlementTime;
    private final MaxNumberCheckerCore maxAccountCount = new MaxNumberCheckerCore(1);
    private final MaxNumberCheckerCore maxInitialBalance = new MaxNumberCheckerCore(Double.MAX_VALUE);

    public UserSettingCore() {
        settlementTime = new EveryTimeCheckerCore();
    }

    public UserSettingCore(UserSettingCore userSetting) {
        settlementTime = new EveryTimeCheckerCore(userSetting.settlementTime());
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

    @Override
    public EveryTimeChecker settlementTime() {
        return settlementTime;
    }

}
