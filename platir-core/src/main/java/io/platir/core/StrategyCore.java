package io.platir.core;

import io.platir.Strategy;
import io.platir.Transaction;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

class StrategyCore implements Strategy {

    private String createDate;
    private String removeDate;
    private String password;
    private String strategyId;
    private String state;
    private final Map<String, String> parameters = new HashMap<>();
    private final Map<String, TransactionCore> transactionMap = new HashMap<>();
    private AccountCore account;

    @Override
    public String getCreateDate() {
        return createDate;
    }

    void setCreateDate(String createDate) {
        this.createDate = createDate;
    }

    @Override
    public String getRemoveDate() {
        return removeDate;
    }

    void setRemoveDate(String removeDate) {
        this.removeDate = removeDate;
    }

    @Override
    public String getPassword() {
        return password;
    }

    void setPassword(String password) {
        this.password = password;
    }

    @Override
    public String getStrategyId() {
        return strategyId;
    }

    void setStrategyId(String strategyId) {
        this.strategyId = strategyId;
    }

    @Override
    public String getState() {
        return state;
    }

    void setState(String state) {
        this.state = state;
    }

    @Override
    public AccountCore getAccount() {
        return account;
    }

    @Override
    public Map<String, String> getParameters() {
        return new HashMap<>(parameters);
    }

    void setParameters(Map<String, String> parameters) {
        this.parameters.putAll(parameters);
    }

    @Override
    public Collection<Transaction> getTransactions() {
        return transactionMap.values().stream().map(core -> {
            return (Transaction) core;
        }).collect(Collectors.toSet());
    }

    Map<String, TransactionCore> transactionMap() {
        return transactionMap;
    }

    void setAccount(AccountCore account) {
        this.account = account;
    }

}
