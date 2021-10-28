package io.platir.engine.core;

import io.platir.engine.SettingFactory;
import io.platir.engine.AccountSetting;
import io.platir.engine.StrategySetting;
import io.platir.engine.UserSetting;

class SettingFactoryCore implements SettingFactory {

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
