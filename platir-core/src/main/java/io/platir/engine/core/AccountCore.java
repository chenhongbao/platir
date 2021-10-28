package io.platir.engine.core;

import com.google.gson.annotations.Expose;
import io.platir.Account;
import io.platir.Contract;
import io.platir.Strategy;
import io.platir.engine.AccountSetting;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

class AccountCore implements Account {

    private String accountId;
    private Double openingMargin;
    private Double openingCommission;
    private Double closingCommission;
    private Double balance;
    private Double margin;
    private Double commission;
    private Double available;
    private Double positionProfit;
    private Double closeProfit;
    private Double ydBalance;
    private String tradingDay;
    private String settleDatetime;
    private UserCore user;
    private AccountSetting accountSetting;
    private final Map<String, StrategyCore> strategies = new ConcurrentHashMap<>();
    private final Map<String, ContractCore> contracts = new ConcurrentHashMap<>();

    @Expose(serialize = false, deserialize = false)
    private final Object syncObject = new Object();

    Object syncObject() {
        return syncObject;
    }

    AccountSetting getAccountSetting() {
        return accountSetting;
    }

    void setAccountRule(AccountSettingCore accountSetting) {
        this.accountSetting = new AccountSettingCore(accountSetting);
    }

    @Override
    public String getAccountId() {
        return accountId;
    }

    void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    @Override
    public Double getOpeningMargin() {
        return openingMargin;
    }

    void setOpeningMargin(Double openingMargin) {
        this.openingMargin = openingMargin;
    }

    @Override
    public Double getOpeningCommission() {
        return openingCommission;
    }

    void setOpeningCommission(Double openingCommission) {
        this.openingCommission = openingCommission;
    }

    @Override
    public Double getClosingCommission() {
        return closingCommission;
    }

    void setClosingCommission(Double closingCommission) {
        this.closingCommission = closingCommission;
    }

    @Override
    public Double getBalance() {
        return balance;
    }

    void setBalance(Double balance) {
        this.balance = balance;
    }

    @Override
    public Double getMargin() {
        return margin;
    }

    void setMargin(Double margin) {
        this.margin = margin;
    }

    @Override
    public Double getCommission() {
        return commission;
    }

    void setCommission(Double commission) {
        this.commission = commission;
    }

    @Override
    public Double getAvailable() {
        return available;
    }

    void setAvailable(Double available) {
        this.available = available;
    }

    @Override
    public Double getPositionProfit() {
        return positionProfit;
    }

    void setPositionProfit(Double positionProfit) {
        this.positionProfit = positionProfit;
    }

    @Override
    public Double getCloseProfit() {
        return closeProfit;
    }

    void setCloseProfit(Double closeProfit) {
        this.closeProfit = closeProfit;
    }

    @Override
    public Double getYdBalance() {
        return ydBalance;
    }

    void setYdBalance(Double ydBalance) {
        this.ydBalance = ydBalance;
    }

    @Override
    public String getTradingDay() {
        return tradingDay;
    }

    void setTradingDay(String tradingDay) {
        this.tradingDay = tradingDay;
    }

    @Override
    public String getSettleDatetime() {
        return settleDatetime;
    }

    void setSettleDatetime(String datetime) {
        this.settleDatetime = datetime;
    }

    @Override
    public UserCore getUser() {
        return user;
    }

    @Override
    public Collection<Strategy> getStrategies() {
        return strategies.values().stream().map(core -> {
            return (Strategy) core;
        }).collect(Collectors.toSet());
    }

    @Override
    public Collection<Contract> getContracts() {
        return contracts.values().stream().map(core -> {
            return (Contract) core;
        }).collect(Collectors.toSet());
    }

    Map<String, StrategyCore> strategies() {
        return strategies;
    }

    Map<String, ContractCore> contracts() {
        return contracts;
    }

    void setUser(UserCore user) {
        this.user = user;
    }

}
