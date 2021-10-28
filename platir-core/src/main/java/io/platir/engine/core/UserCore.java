package io.platir.engine.core;

import io.platir.Account;
import io.platir.User;
import io.platir.engine.UserSetting;
import io.platir.engine.setting.SettingFactory;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

class UserCore implements User {

    private String userId;
    private String password;
    private String createDatetime;
    private String lastLoginDatetime;
    private UserSetting userSetting;
    private final Map<String, AccountCore> accounts = new ConcurrentHashMap<>();

    UserSetting getUserSetting() {
        return userSetting;
    }

    void setUserSetting(UserSetting userSetting) {
        this.userSetting = SettingFactory.newUserSetting(userSetting);
    }

    @Override
    public String getUserId() {
        return userId;
    }

    void setUserId(String userId) {
        this.userId = userId;
    }

    @Override
    public String getPassword() {
        return password;
    }

    void setPassword(String password) {
        this.password = password;
    }

    @Override
    public String getCreateDatetime() {
        return createDatetime;
    }

    void setCreateDatetime(String datetime) {
        this.createDatetime = datetime;
    }

    @Override
    public String getLastLoginDatetime() {
        return lastLoginDatetime;
    }

    void setLastLoginDatetime(String datetime) {
        this.lastLoginDatetime = datetime;
    }

    @Override
    public Collection<Account> getAccounts() {
        return accounts.values().stream()
                .map(core -> {
                    return (Account) core;
                })
                .collect(Collectors.toSet());
    }

    Map<String, AccountCore> accounts() {
        return accounts;
    }

}
