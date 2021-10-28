package io.platir.engine.core;

import com.google.gson.annotations.Expose;
import io.platir.Strategy;
import io.platir.Transaction;
import io.platir.engine.StrategySetting;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

class StrategyCore implements Strategy {

    private String createDatetime;
    private String removeDatetime;
    private String strategyId;
    private String state;
    private final Map<String, TransactionCore> transactions = new ConcurrentHashMap<>();
    private StrategySetting strategySetting;
    private AccountCore account;

    @Expose(serialize = false, deserialize = false)
    private final Object syncObject = new Object();

    Object syncObject() {
        return syncObject;
    }

    StrategySetting getStrategySetting() {
        return strategySetting;
    }

    void setStrategySetting(StrategySettingCore strategySetting) {
        this.strategySetting = new StrategySettingCore(strategySetting);
    }

    @Override
    public String getCreateDatetime() {
        return createDatetime;
    }

    void setCreateDatetime(String datetime) {
        this.createDatetime = datetime;
    }

    @Override
    public String getRemoveDatetime() {
        return removeDatetime;
    }

    void setRemoveDatetime(String dateTime) {
        this.removeDatetime = dateTime;
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
        return transactions.values().stream().map(core -> {
            return (Transaction) core;
        }).collect(Collectors.toSet());
    }

    Map<String, TransactionCore> transactions() {
        return transactions;
    }

    void setAccount(AccountCore account) {
        this.account = account;
    }

}
