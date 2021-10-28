package io.platir.commons;

import io.platir.Account;
import io.platir.User;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class UserCore implements User {

    private String userId;
    private String password;
    private String createDatetime;
    private String lastLoginDatetime;
    private UserSettingCore userSetting;
    private final Map<String, AccountCore> accounts = new ConcurrentHashMap<>();

    public UserSettingCore getUserSetting() {
        return userSetting;
    }

    public void setUserSetting(UserSettingCore userSetting) {
        this.userSetting = new UserSettingCore(userSetting);
    }

    @Override
    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    @Override
    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    @Override
    public String getCreateDatetime() {
        return createDatetime;
    }

    public void setCreateDatetime(String datetime) {
        this.createDatetime = datetime;
    }

    @Override
    public String getLastLoginDatetime() {
        return lastLoginDatetime;
    }

    public void setLastLoginDatetime(String datetime) {
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

    public Map<String, AccountCore> accounts() {
        return accounts;
    }

}
