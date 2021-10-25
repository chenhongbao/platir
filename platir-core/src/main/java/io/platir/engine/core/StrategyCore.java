package io.platir.engine.core;

import io.platir.Strategy;
import io.platir.Transaction;
import io.platir.engine.rule.StrategyRule;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

class StrategyCore implements Strategy {

    private String createDate;
    private String removeDate;
    private String strategyId;
    private String state;
    private final Map<String, TransactionCore> transactionMap = new HashMap<>();
    private StrategyRule strategyRule;
    private AccountCore account;

    StrategyRule getStrategyRule() {
        return strategyRule;
    }

    void setStrategyRule(StrategyRule strategyRule) {
        this.strategyRule = new StrategyRule(strategyRule);
    }

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
