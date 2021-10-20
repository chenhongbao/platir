/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package io.platir.service;

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
import java.util.Set;

/**
 *
 * @author chenh
 */
public interface Schema {

    Set<Account> getAccounts();

    void setAccounts(Set<Account> accounts);

    Set<Contract> getContracts();

    void setContracts(Set<Contract> contracts);

    Set<Instrument> getInstruments();

    void setInstruments(Set<Instrument> instruments);

    String getLastModifiedTime();

    void setLastModifiedTime(String lastModifiedTime);

    Set<Order> getOrders();

    void setOrders(Set<Order> orders);

    Set<StrategyProfile> getStrategyProfiles();

    void setStrategyProfiles(Set<StrategyProfile> strategyProfiles);

    Set<Tick> getTicks();

    void setTicks(Set<Tick> ticks);

    Set<Trade> getTrades();

    void setTrades(Set<Trade> trades);

    TradingDay getTradingDay();

    void setTradingDay(TradingDay tradingDay);

    Set<Transaction> getTransactions();

    void setTransactions(Set<Transaction> transactions);

    Set<User> getUsers();

    void setUsers(Set<User> users);
    
}
