package io.platir;

import java.util.Collection;
import java.util.Map;

public interface Strategy {

    String getCreateDate();

    String getRemoveDate();

    String getStrategyId();

    String getState();

    Collection<Transaction> getTransactions();

    Account getAccount();
}
