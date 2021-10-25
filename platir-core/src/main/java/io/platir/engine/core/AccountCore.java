package io.platir.engine.core;

import io.platir.Account;
import io.platir.Contract;
import io.platir.Strategy;
import io.platir.User;
import io.platir.engine.rule.AccountRule;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
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
    private String settleTime;
    private final Map<String, StrategyCore> strategyMap = new HashMap<>();
    private final Map<String, ContractCore> contractMap = new HashMap<>();
    private UserCore user;
    private AccountRule accountRule;

    AccountRule getAccountRule() {
        return accountRule;
    }

    void setAccountRule(AccountRule accountRule) {
        this.accountRule = new AccountRule(accountRule);
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
    public String getSettleTime() {
        return settleTime;
    }

    void setSettleTime(String settleTime) {
        this.settleTime = settleTime;
    }

    @Override
    public User getUser() {
        return user;
    }

    @Override
    public Collection<Strategy> getStrategies() {
        return strategyMap.values().stream().map(core -> {
            return (Strategy) core;
        }).collect(Collectors.toSet());
    }

    @Override
    public Collection<Contract> getContracts() {
        return contractMap.values().stream().map(core -> {
            return (Contract) core;
        }).collect(Collectors.toSet());
    }

    Map<String, StrategyCore> strategyMap() {
        return strategyMap;
    }

    Map<String, ContractCore> contractMap() {
        return contractMap;
    }

    void setUser(UserCore user) {
        this.user = user;
    }

}
