package io.platir.core.internals;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import io.platir.core.PlatirSystem;
import io.platir.service.OrderContext;
import io.platir.service.Tick;
import io.platir.service.Transaction;
import io.platir.service.TransactionContext;

class TransactionContextImpl implements TransactionContext {

    private final Transaction trans;
    private final StrategyContextImpl stg;
    private final AtomicBoolean awaken = new AtomicBoolean(false);
    private final AtomicReference<Tick> triggerTick = new AtomicReference<>();
    private final Set<OrderContextImpl> pending = new ConcurrentSkipListSet<>();
    private final Set<OrderContextImpl> orders = new ConcurrentSkipListSet<>();
    private final Lock l = new ReentrantLock();
    private final Condition cond = l.newCondition();

    TransactionContextImpl(Transaction transaction, StrategyContextImpl strategy) {
        trans = transaction;
        stg = strategy;
    }

    PlatirInfoClientImpl getQueryClient() {
        return stg.getPlatirClientImpl();
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
        return pending;
    }

    @Override
    public StrategyContextImpl getStrategyContext() {
        return stg;
    }

    @Override
    public Transaction getTransaction() {
        return trans;
    }

    @Override
    public void join() {
        while (!awaken.get()) {
            l.lock();
            try {
                cond.await();
            } catch (InterruptedException e) {
                PlatirSystem.err.write("Joining transaction is interrupted.", e);
            } finally {
                l.unlock();
            }
        }
    }

    public void awake() {
        if (awaken.get()) {
            return;
        }
        awaken.set(true);
        l.lock();
        try {
            cond.signal();
        } finally {
            l.unlock();
        }
    }

    @Override
    public Set<OrderContext> getOrderContexts() {
        return new HashSet<>(orders);
    }

}
