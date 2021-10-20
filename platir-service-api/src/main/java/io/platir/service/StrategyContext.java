package io.platir.service;

import java.util.List;
import java.util.Set;
import java.util.logging.LogRecord;

public interface StrategyContext {

	void start();

	void stop(int reason);

	void interruptTransaction(boolean interrupted) throws InterruptionException;

	StrategyProfile getProfile();

	Object getStrategy();

	PlatirClient getPlatirClient();

	Set<TransactionContext> getTransactionContexts();
        
        List<LogRecord> getLogs();
        
        void clearLogs();
}
