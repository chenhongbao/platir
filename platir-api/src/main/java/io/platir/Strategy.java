package io.platir;

import java.util.Collection;

public interface Strategy {

    public static final String NORMAL = "NORMAL";
    public static final String BLOCKED = "BLOCKED";
    public static final String REMOVED = "REMOVED";

    String getCreateDate();

    String getRemoveDate();

    String getStrategyId();

    String getState();

    Collection<Transaction> getTransactions();

    Account getAccount();
}
