package io.platir.service;

import java.util.Set;

public interface TransactionContext {
	Transaction getTransaction();
	
	Set<OrderContext> getOrderContexts();
	
	void join();
}
