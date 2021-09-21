package io.platir.service.api;

import java.sql.SQLException;
import java.util.Set;

import io.platir.service.Account;
import io.platir.service.Contract;
import io.platir.service.Instrument;
import io.platir.service.Order;
import io.platir.service.RiskNotice;
import io.platir.service.StrategyProfile;
import io.platir.service.Tick;
import io.platir.service.Trade;
import io.platir.service.Transaction;
import io.platir.service.User;

public interface Queries {

	void prepareTables() throws SQLException;

	void insert(String tradingDay) throws SQLException;

	void insert(Account... accounts) throws SQLException;

	void insert(Tick... ticks) throws SQLException;

	void insert(Transaction... transactions) throws SQLException;

	void insert(Order... orders) throws SQLException;

	void insert(Trade... trades) throws SQLException;

	void insert(Contract... contracts) throws SQLException;

	void insert(User... users) throws SQLException;

	void insert(StrategyProfile... profiles) throws SQLException;

	void insert(Instrument... instruments) throws SQLException;

	void insert(RiskNotice... notices) throws SQLException;

	void update(Account... accounts) throws SQLException;

	void update(Contract... contracts) throws SQLException;

	void update(Transaction... transactions) throws SQLException;

	void update(Instrument... instruments) throws SQLException;

	void update(User... users) throws SQLException;

	void update(StrategyProfile... profiles) throws SQLException;

	void updateTradingDay(String tradingDay) throws SQLException;

	void clearAccounts() throws SQLException;

	void clearContracts() throws SQLException;

	void clearOrders() throws SQLException;

	void clearTrades() throws SQLException;

	void clearTransactions() throws SQLException;

	void clearTicks() throws SQLException;

	void clearStrategies() throws SQLException;

	String selectTradingday() throws SQLException;

	Set<Account> selectAccounts() throws SQLException;

	Set<Contract> selectContracts() throws SQLException;

	Set<Instrument> selectInstruments() throws SQLException;

	Set<Order> selectOrders() throws SQLException;

	Set<StrategyProfile> selectStrategyProfiles() throws SQLException;

	Set<Trade> selectTrades() throws SQLException;

	Set<Transaction> selectTransactions() throws SQLException;

	Set<User> selectUsers() throws SQLException;

	Set<Tick> selectTicks() throws SQLException;

}
