package io.platir.service;

import java.util.logging.Logger;
import java.util.Set;

public interface PlatirInfoClient {
	String getStrategyId();

	Account getAccount();
	
	StrategyProfile getStrategyProfile();
	
	Set<Transaction> getTransactions();

	Set<Order> getOrders(String transactionId);

	Set<Trade> getTrades(String orderId);

	Instrument getInstrument(String instrumentId);

	Set<Position> getPositions(String... instrumentIds);

	Set<Contract> getContracts(String... instrumentIds);

	String getTradingDay();

        Logger getLogger();
}