package io.platir.core.internals;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;

import io.platir.service.Contract;
import io.platir.service.Order;
import io.platir.service.OrderContext;
import io.platir.service.Trade;

class OrderContextImpl implements OrderContext {
	
	private final Order o;
	private final Set<Contract> locked = new ConcurrentSkipListSet<>();
	private final Set<Trade> trades = new ConcurrentSkipListSet<>();
	
	OrderContextImpl(Order order) {
		o = order;
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

}
