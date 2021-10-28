package io.platir.setting;

public interface UserSetting {

    MaxNumberChecker maxAccountCount();

    MaxNumberChecker maxInitialBalance();
    
    EveryTimeChecker settlementTime();

}
