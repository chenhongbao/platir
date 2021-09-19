package io.platir.service;

import java.util.Set;

public interface StrategyContext {

	void initialize();

	void shutdown(int reason);

	void destroy(int reason) throws StrategyDrestroyException;

	StrategyProfile getPofile();

	Object getStrategy();

	PlatirClient getPlatirClient();

	Set<TransactionContext> getTransactionContexts();
}
