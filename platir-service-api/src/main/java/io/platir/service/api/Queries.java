package io.platir.service.api;

import java.util.Set;

import io.platir.service.Account;
import io.platir.service.Contract;
import io.platir.service.Instrument;
import io.platir.service.Order;
import io.platir.service.RiskNotice;
import io.platir.service.StrategyProfile;
import io.platir.service.Tick;
import io.platir.service.Trade;
import io.platir.service.TradingDay;
import io.platir.service.Transaction;
import io.platir.service.User;
import java.io.File;

public interface Queries {

    void initialize() throws DataQueryException;

    void destroy() throws DataQueryException;
    
    void backup(File target);

    void insert(TradingDay day) throws DataQueryException;

    void insert(Account... accounts) throws DataQueryException;

    void insert(Tick... ticks) throws DataQueryException;

    void insert(Transaction... transactions) throws DataQueryException;

    void insert(Order... orders) throws DataQueryException;

    void insert(Trade... trades) throws DataQueryException;

    void insert(Contract... contracts) throws DataQueryException;

    void insert(User... users) throws DataQueryException;

    void insert(StrategyProfile... profiles) throws DataQueryException;

    void insert(Instrument... instruments) throws DataQueryException;

    void insert(RiskNotice... notices) throws DataQueryException;

    void update(Account... accounts) throws DataQueryException;

    void update(Contract... contracts) throws DataQueryException;

    void update(Transaction... transactions) throws DataQueryException;

    void update(Instrument... instruments) throws DataQueryException;

    void update(User... users) throws DataQueryException;

    void update(StrategyProfile... profiles) throws DataQueryException;

    void updateTradingDay(TradingDay day) throws DataQueryException;

    void clearAccounts() throws DataQueryException;

    void clearContracts() throws DataQueryException;

    void clearOrders() throws DataQueryException;

    void clearTrades() throws DataQueryException;

    void clearTransactions() throws DataQueryException;

    void clearTicks() throws DataQueryException;

    void clearStrategies() throws DataQueryException;
    
    void clearRiskNotices() throws DataQueryException;

    TradingDay selectTradingDay() throws DataQueryException;

    Set<Account> selectAccounts() throws DataQueryException;

    Set<Contract> selectContracts() throws DataQueryException;

    Set<Instrument> selectInstruments() throws DataQueryException;

    Set<Order> selectOrders() throws DataQueryException;

    Set<StrategyProfile> selectStrategyProfiles() throws DataQueryException;

    Set<Trade> selectTrades() throws DataQueryException;

    Set<Transaction> selectTransactions() throws DataQueryException;

    Set<User> selectUsers() throws DataQueryException;

    Set<Tick> selectTicks() throws DataQueryException;

}
