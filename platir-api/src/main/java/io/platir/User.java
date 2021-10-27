package io.platir;

import java.util.Collection;

public interface User {

    String getUserId();

    String getPassword();

    String getCreateDatetime();

    String getLastLoginDatetime();

    Collection<Account> getAccounts();
}
