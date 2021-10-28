package io.platir.commons;

import io.platir.Strategy;
import io.platir.Transaction;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class StrategyCore implements Strategy {

    private String createDatetime;
    private String removeDatetime;
    private String strategyId;
    private String state;
    private StrategySettingCore strategySetting;
    private AccountCore account;
    private final Map<String, TransactionCore> transactions = new ConcurrentHashMap<>();
    private final Object syncObject = new Object();

    public Object syncObject() {
        return syncObject;
    }

    public StrategySettingCore getStrategySetting() {
        return strategySetting;
    }

    public void setStrategySetting(StrategySettingCore strategySetting) {
        this.strategySetting = new StrategySettingCore(strategySetting);
    }

    @Override
    public String getCreateDatetime() {
        return createDatetime;
    }

    public void setCreateDatetime(String datetime) {
        this.createDatetime = datetime;
    }

    @Override
    public String getRemoveDatetime() {
        return removeDatetime;
    }

    public void setRemoveDatetime(String dateTime) {
        this.removeDatetime = dateTime;
    }

    @Override
    public String getStrategyId() {
        return strategyId;
    }

    public void setStrategyId(String strategyId) {
        this.strategyId = strategyId;
    }

    @Override
    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    @Override
    public AccountCore getAccount() {
        return account;
    }

    @Override
    public Collection<Transaction> getTransactions() {
        return transactions.values().stream().map(core -> {
            return (Transaction) core;
        }).collect(Collectors.toSet());
    }

    public Map<String, TransactionCore> transactions() {
        return transactions;
    }

    public void setAccount(AccountCore account) {
        this.account = account;
    }

}
