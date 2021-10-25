package io.platir;

import java.util.Collection;

public interface User {

    String getUserId();

    String getPassword();

    String getCreateTime();

    String getLastLoginTime();

    Collection<Account> getAccounts();
}
