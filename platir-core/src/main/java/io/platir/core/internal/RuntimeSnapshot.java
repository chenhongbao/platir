package io.platir.core.internal;

import java.util.HashSet;
import java.util.Set;

import io.platir.service.Account;
import io.platir.service.Contract;
import io.platir.service.Instrument;
import io.platir.service.Order;
import io.platir.service.StrategyProfile;
import io.platir.service.Trade;
import io.platir.service.TradingDay;
import io.platir.service.Transaction;
import io.platir.service.User;

public class RuntimeSnapshot {

    private TradingDay tradingDay;
    private final Set<Account> accounts = new HashSet<>();
    private final Set<Contract> contracts = new HashSet<>();
    private final Set<Instrument> instruments = new HashSet<>();
    private final Set<Order> orders = new HashSet<>();
    private final Set<StrategyProfile> strategyProfiles = new HashSet<>();
    private final Set<Trade> trades = new HashSet<>();
    private final Set<Transaction> transactions = new HashSet<>();
    private final Set<User> users = new HashSet<>();

    public TradingDay getTradingDay() {
        return tradingDay;
    }

    public void setTradingDay(TradingDay tradingDay) {
        this.tradingDay = tradingDay;
    }

    public RuntimeSnapshot() {
    }

    public Set<Account> accounts() {
        return accounts;
    }

    public Set<Contract> contracts() {
        return contracts;
    }

    public Set<Instrument> instruments() {
        return instruments;
    }

    public Set<Order> orders() {
        return orders;
    }

    public Set<StrategyProfile> strategyProfiles() {
        return strategyProfiles;
    }

    public Set<Trade> trades() {
        return trades;
    }

    public Set<Transaction> transactions() {
        return transactions;
    }

    public Set<User> users() {
        return users;
    }

}
