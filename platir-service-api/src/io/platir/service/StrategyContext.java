package io.platir.service;

import java.util.Set;


public interface StrategyContext {

	void initialize();

	void shutdown(int reason);

	void remove(int reason) throws StrategyRemovalException, InvalidLoginException;

	StrategyProfile getProfile();

	Object getStrategy();

	PlatirClient getPlatirClient();

	Set<TransactionContext> getTransactionContexts();
}
