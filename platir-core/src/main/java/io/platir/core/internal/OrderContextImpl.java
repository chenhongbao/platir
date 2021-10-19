package io.platir.core.internal;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;

import io.platir.service.Contract;
import io.platir.service.Order;
import io.platir.service.OrderContext;
import io.platir.service.Trade;
import io.platir.service.TransactionContext;

class OrderContextImpl implements OrderContext {

    private final Order order;
    private final TransactionContextImpl transactionContext;
    private final Set<Contract> lockedContracts = new ConcurrentSkipListSet<>();
    private final Set<Trade> tradedRecords = new ConcurrentSkipListSet<>();

    OrderContextImpl(Order order, TransactionContextImpl transactionContext) {
        this.order = order;
        this.transactionContext = transactionContext;
    }

    void addTrade(Trade trade) {
        tradedRecords.add(trade);
    }

    @Override
    public Order getOrder() {
        return order;
    }

    @Override
    public Set<Contract> lockedContracts() {
        return lockedContracts;
    }

    @Override
    public Set<Trade> getTrades() {
        return new HashSet<>(tradedRecords);
    }

    @Override
    public TransactionContext getTransactionContext() {
        return transactionContext;
    }

}
