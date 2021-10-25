package io.platir.engine.core;

import io.platir.Account;
import io.platir.User;
import io.platir.engine.rule.UserRule;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

class UserCore implements User {

    private String userId;
    private String password;
    private String createTime;
    private String lastLoginTime;
    private UserRule userRule;
    private final Map<String, AccountCore> accountMap = new HashMap<>();

    UserRule getUserRule() {
        return userRule;
    }

    void setUserRule(UserRule userRule) {
        this.userRule = new UserRule(userRule);
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
    public String getCreateTime() {
        return createTime;
    }

    void setCreateTime(String createTime) {
        this.createTime = createTime;
    }

    @Override
    public String getLastLoginTime() {
        return lastLoginTime;
    }

    void setLastLoginTime(String lastLoginTime) {
        this.lastLoginTime = lastLoginTime;
    }

    @Override
    public Collection<Account> getAccounts() {
        return accountMap.values().stream()
                .map(core -> {
                    return (Account) core;
                })
                .collect(Collectors.toSet());
    }

    Map<String, AccountCore> accountMap() {
        return accountMap;
    }

}
