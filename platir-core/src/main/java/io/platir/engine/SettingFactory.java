package io.platir.engine;

public interface SettingFactory {

    AccountSetting newAccountSetting();

    StrategySetting newStrategySetting();

    UserSetting newUserSetting();

}
