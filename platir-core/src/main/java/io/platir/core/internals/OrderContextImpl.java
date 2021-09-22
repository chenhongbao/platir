package io.platir.core.internals;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;

import io.platir.service.Contract;
import io.platir.service.Order;
import io.platir.service.OrderContext;
import io.platir.service.Trade;
import io.platir.service.TransactionContext;

class OrderContextImpl implements OrderContext {
	
	private final Order o;
	private final TransactionContextImpl trCtx;
	private final Set<Contract> locked = new ConcurrentSkipListSet<>();
	private final Set<Trade> trades = new ConcurrentSkipListSet<>();
	
	OrderContextImpl(Order order, TransactionContextImpl transaction) {
		o = order;
		trCtx = transaction;
	}
	
	void addTrade(Trade trade) {
		trades.add(trade);
	}

	@Override
	public Order getOrder() {
		return o;
	}

	@Override
	public Set<Contract> lockedContracts() {
		return locked;
	}

	@Override
	public Set<Trade> getTrades() {
		return new HashSet<>(trades);
	}

	@Override
	public TransactionContext getTransactionContext() {
		return trCtx;
	}

}
