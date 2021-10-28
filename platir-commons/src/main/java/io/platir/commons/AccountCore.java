package io.platir.commons;

import com.google.gson.annotations.Expose;
import io.platir.Account;
import io.platir.Contract;
import io.platir.Strategy;
import io.platir.setting.AccountSetting;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class AccountCore implements Account {

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
    private String state;
    private AccountSettingCore accountSetting;
    private final Map<String, StrategyCore> strategies = new ConcurrentHashMap<>();
    private final Map<String, ContractCore> contracts = new ConcurrentHashMap<>();

    @Expose(serialize = false, deserialize = false)
    private UserCore user;

    private final Object syncObject = new Object();

    public Object syncObject() {
        return syncObject;
    }

    @Override
    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public AccountSettingCore getAccountSetting() {
        return accountSetting;
    }

    public void setAccountRule(AccountSetting accountSetting) {
        this.accountSetting = new AccountSettingCore((AccountSettingCore) accountSetting);
    }

    @Override
    public String getAccountId() {
        return accountId;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    @Override
    public Double getOpeningMargin() {
        return openingMargin;
    }

    public void setOpeningMargin(Double openingMargin) {
        this.openingMargin = openingMargin;
    }

    @Override
    public Double getOpeningCommission() {
        return openingCommission;
    }

    public void setOpeningCommission(Double openingCommission) {
        this.openingCommission = openingCommission;
    }

    @Override
    public Double getClosingCommission() {
        return closingCommission;
    }

    public void setClosingCommission(Double closingCommission) {
        this.closingCommission = closingCommission;
    }

    @Override
    public Double getBalance() {
        return balance;
    }

    public void setBalance(Double balance) {
        this.balance = balance;
    }

    @Override
    public Double getMargin() {
        return margin;
    }

    public void setMargin(Double margin) {
        this.margin = margin;
    }

    @Override
    public Double getCommission() {
        return commission;
    }

    public void setCommission(Double commission) {
        this.commission = commission;
    }

    @Override
    public Double getAvailable() {
        return available;
    }

    public void setAvailable(Double available) {
        this.available = available;
    }

    @Override
    public Double getPositionProfit() {
        return positionProfit;
    }

    public void setPositionProfit(Double positionProfit) {
        this.positionProfit = positionProfit;
    }

    @Override
    public Double getCloseProfit() {
        return closeProfit;
    }

    public void setCloseProfit(Double closeProfit) {
        this.closeProfit = closeProfit;
    }

    @Override
    public Double getYdBalance() {
        return ydBalance;
    }

    public void setYdBalance(Double ydBalance) {
        this.ydBalance = ydBalance;
    }

    @Override
    public String getTradingDay() {
        return tradingDay;
    }

    public void setTradingDay(String tradingDay) {
        this.tradingDay = tradingDay;
    }

    @Override
    public String getSettleDatetime() {
        return settleDatetime;
    }

    public void setSettleDatetime(String datetime) {
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

    public Map<String, StrategyCore> strategies() {
        return strategies;
    }

    public Map<String, ContractCore> contracts() {
        return contracts;
    }

    public void setUser(UserCore user) {
        this.user = user;
    }

}
