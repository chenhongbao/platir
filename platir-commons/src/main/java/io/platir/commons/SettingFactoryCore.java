package io.platir.commons;

import io.platir.setting.SettingFactory;
import io.platir.setting.AccountSetting;
import io.platir.setting.StrategySetting;
import io.platir.setting.UserSetting;

public class SettingFactoryCore implements SettingFactory {

    @Override
    public AccountSetting newAccountSetting() {
        return new AccountSettingCore();
    }

    @Override
    public StrategySetting newStrategySetting() {
        return new StrategySettingCore();
    }

    @Override
    public UserSetting newUserSetting() {
        return new UserSettingCore();
    }

}
