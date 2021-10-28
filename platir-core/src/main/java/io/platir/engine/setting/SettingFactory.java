package io.platir.engine.setting;

import io.platir.engine.AccountSetting;
import io.platir.engine.StrategySetting;
import io.platir.engine.UserSetting;

public class SettingFactory {

    public static AccountSetting newAccountSetting() {
        return new AccountSettingCore();
    }

    public static AccountSetting newAccountSetting(AccountSetting accountSetting) {
        return new AccountSettingCore(accountSetting);
    }

    public static StrategySetting newStrategySetting() {
        return new StrategySettingCore();
    }

    public static StrategySetting newStrategySetting(StrategySetting strategySetting) {
        return new StrategySettingCore(strategySetting);
    }

    public static UserSetting newUserSetting() {
        return new UserSettingCore();
    }
    
        public static UserSetting newUserSetting(UserSetting userSetting) {
        return new UserSettingCore(userSetting);
    }
}
