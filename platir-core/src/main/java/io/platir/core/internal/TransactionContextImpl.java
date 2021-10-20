package io.platir.core.internal;

import io.platir.queries.Utils;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import io.platir.service.OrderContext;
import io.platir.service.Tick;
import io.platir.service.Transaction;
import io.platir.service.TransactionContext;

class TransactionContextImpl implements TransactionContext {

    private final Transaction transaction;
    private final StrategyContextImpl strategyContext;
    private final AtomicBoolean isAwaken = new AtomicBoolean(false);
    private final AtomicReference<Tick> triggerTick = new AtomicReference<>();
    private final Set<OrderContextImpl> pendingOrder = new ConcurrentSkipListSet<>();
    private final Set<OrderContextImpl> orders = new ConcurrentSkipListSet<>();
    private final Lock completionLock = new ReentrantLock();
    private final Condition conpletionCondition = completionLock.newCondition();

    TransactionContextImpl(Transaction transaction, StrategyContextImpl strategyContext) {
        this.transaction = transaction;
        this.strategyContext = strategyContext;
    }

    PlatirInfoClientImpl getQueryClient() {
        return strategyContext.getPlatirClientImpl();
    }

    Tick getLastTriggerTick() {
        return triggerTick.get();
    }

    void setTriggerTick(Tick tick) {
        triggerTick.set(tick);
    }

    void addOrderContext(OrderContextImpl order) {
        orders.add(order);
    }

    Set<OrderContextImpl> pendingOrder() {
        return pendingOrder;
    }

    @Override
    public StrategyContextImpl getStrategyContext() {
        return strategyContext;
    }

    @Override
    public Transaction getTransaction() {
        return transaction;
    }

    @Override
    public void join() {
        while (!isAwaken.get()) {
            completionLock.lock();
            try {
                conpletionCondition.await();
            } catch (InterruptedException e) {
                Utils.err().write("Joining transaction is interrupted.", e);
            } finally {
                completionLock.unlock();
            }
        }
    }

    public void awake() {
        if (isAwaken.get()) {
            return;
        }
        isAwaken.set(true);
        completionLock.lock();
        try {
            conpletionCondition.signal();
        } finally {
            completionLock.unlock();
        }
    }

    @Override
    public Set<OrderContext> getOrderContexts() {
        return new HashSet<>(orders);
    }

}
