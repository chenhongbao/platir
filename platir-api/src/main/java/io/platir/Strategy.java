package io.platir;

import java.util.Collection;
import java.util.Map;

public interface Strategy {

    String getCreateDate();

    String getRemoveDate();

    void setCreateDate(String createDate);

    void setRemoveDate(String removeDate);

    String getPassword();

    void setPassword(String password);

    String getStrategyId();

    void setStrategyId(String strategyId);

    String getState();

    void setState(String state);

    Map<String, String> getParameters();
    
    void setParameters(Map<String, String> parameters);
    
    Collection<Transaction> getTransactions();
    
    void addTransaction(Transaction transaction);
    
    Account getAccount();
}
