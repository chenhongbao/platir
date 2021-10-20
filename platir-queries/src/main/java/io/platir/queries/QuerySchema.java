package io.platir.queries;

import io.platir.service.Schema;
import io.platir.service.Account;
import io.platir.service.Contract;
import io.platir.service.Instrument;
import io.platir.service.Order;
import io.platir.service.StrategyProfile;
import io.platir.service.Tick;
import io.platir.service.Trade;
import io.platir.service.TradingDay;
import io.platir.service.Transaction;
import io.platir.service.User;
import java.util.HashSet;
import java.util.Set;

/**
 *
 * @author Chen Hongbao
 */
public class QuerySchema implements Schema {

    private String lastModifiedTime;
    private TradingDay tradingDay;
    private final Set<Account> accounts = new HashSet<>();
    private final Set<Tick> ticks = new HashSet<>();
    private final Set<Transaction> transactions = new HashSet<>();
    private final Set<Order> orders = new HashSet<>();
    private final Set<Trade> trades = new HashSet<>();
    private final Set<Contract> contracts = new HashSet<>();
    private final Set<User> users = new HashSet<>();
    private final Set<StrategyProfile> strategyProfiles = new HashSet<>();
    private final Set<Instrument> instruments = new HashSet<>();

    @Override
    public String getLastModifiedTime() {
        return lastModifiedTime;
    }

    @Override
    public void setLastModifiedTime(String lastModifiedTime) {
        this.lastModifiedTime = lastModifiedTime;
    }

    @Override
    public TradingDay getTradingDay() {
        return tradingDay;
    }

    @Override
    public void setTradingDay(TradingDay tradingDay) {
        this.tradingDay = tradingDay;
    }

    @Override
    public Set<Account> getAccounts() {
        return new HashSet<>(accounts);
    }

    @Override
    public void setAccounts(Set<Account> accounts) {
        this.accounts.clear();
        this.accounts.addAll(accounts);
    }

    @Override
    public Set<Tick> getTicks() {
        return new HashSet<>(ticks);
    }

    @Override
    public void setTicks(Set<Tick> ticks) {
        this.ticks.clear();
        this.ticks.addAll(ticks);
    }

    @Override
    public Set<Transaction> getTransactions() {
        return new HashSet<>(transactions);
    }

    @Override
    public void setTransactions(Set<Transaction> transactions) {
        this.transactions.clear();
        this.transactions.addAll(transactions);
    }

    @Override
    public Set<Order> getOrders() {
        return new HashSet<>(orders);
    }

    @Override
    public void setOrders(Set<Order> orders) {
        this.orders.clear();
        this.orders.addAll(orders);
    }

    @Override
    public Set<Trade> getTrades() {
        return new HashSet<>(trades);
    }

    @Override
    public void setTrades(Set<Trade> trades) {
        this.trades.clear();
        this.trades.addAll(trades);
    }

    @Override
    public Set<Contract> getContracts() {
        return new HashSet<>(contracts);
    }

    @Override
    public void setContracts(Set<Contract> contracts) {
        this.contracts.clear();
        this.contracts.addAll(contracts);
    }

    @Override
    public Set<User> getUsers() {
        return new HashSet<>(users);
    }

    @Override
    public void setUsers(Set<User> users) {
        this.users.clear();
        this.users.addAll(users);
    }

    @Override
    public Set<StrategyProfile> getStrategyProfiles() {
        return new HashSet<>(strategyProfiles);
    }

    @Override
    public void setStrategyProfiles(Set<StrategyProfile> strategyProfiles) {
        this.strategyProfiles.clear();
        this.strategyProfiles.addAll(strategyProfiles);
    }

    @Override
    public Set<Instrument> getInstruments() {
        return new HashSet<>(instruments);
    }

    @Override
    public void setInstruments(Set<Instrument> instruments) {
        this.instruments.clear();
        this.instruments.addAll(instruments);
    }

}
