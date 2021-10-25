package io.platir;

import java.util.Collection;
import java.util.Map;

public interface Strategy {

    String getCreateDate();

    String getRemoveDate();

    String getPassword();

    String getStrategyId();

    String getState();

    Map<String, String> getParameters();

    Collection<Transaction> getTransactions();

    Account getAccount();
}
