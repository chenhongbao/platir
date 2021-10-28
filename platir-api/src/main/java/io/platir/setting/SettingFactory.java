package io.platir.setting;

public interface SettingFactory {

    AccountSetting newAccountSetting();

    StrategySetting newStrategySetting();

    UserSetting newUserSetting();

}
