package io.platir.service;

import java.util.Set;
import java.nio.file.Path;

public interface Queries {

    Factory getFactory();

    void initialize() throws DataQueryException;

    Schema backup(Path directory);

    Schema restore(Path backupDirectory) throws DataQueryException;

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

    void update(Order... orders) throws DataQueryException;

    void update(Transaction... transactions) throws DataQueryException;

    void update(Instrument... instruments) throws DataQueryException;

    void update(User... users) throws DataQueryException;

    void update(StrategyProfile... profiles) throws DataQueryException;

    void update(TradingDay... days) throws DataQueryException;

    void remove(Contract... contracts) throws DataQueryException;

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

    Set<RiskNotice> selectRiskNotices() throws DataQueryException;

}
