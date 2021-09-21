package io.platir.service;

import java.util.Set;

public interface TransactionContext {
	Transaction getTransaction();
	
	StrategyContext getStrategyContext();
	
	Set<OrderContext> getOrderContexts();
	
	void join();
}
