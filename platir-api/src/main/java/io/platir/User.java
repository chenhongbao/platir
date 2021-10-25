package io.platir;

import java.util.Collection;

public interface User {

    String getUserId();

    String getPassword();

    String getCreateTime();

    String getLastLoginTime();

    void setUserId(String userId);

    void setPassword(String password);

    void setCreateTime(String createTime);

    void setLastLoginTime(String lastLoginTime);

    Collection<Account> getAccounts();
    
    void addAccount(Account account);
    
    void removeAccount(String accountId);
}
