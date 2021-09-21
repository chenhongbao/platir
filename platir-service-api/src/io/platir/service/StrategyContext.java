package io.platir.service;

import java.util.Set;

public interface StrategyContext {

	void initialize();

	void shutdown(int reason);
	
	Notice interruptTransaction(boolean interrupted);

	StrategyProfile getProfile();

	Object getStrategy();

	PlatirClient getPlatirClient();

	Set<TransactionContext> getTransactionContexts();
}
