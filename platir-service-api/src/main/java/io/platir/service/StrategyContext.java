package io.platir.service;

import java.util.Set;

public interface StrategyContext {

	void initialize();

	void shutdown(int reason);

	void interruptTransaction(boolean interrupted) throws InterruptionException;

	StrategyProfile getProfile();

	Object getStrategy();

	PlatirClient getPlatirClient();

	Set<TransactionContext> getTransactionContexts();
}
