package io.platir.core.internal;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import io.platir.core.internal.objects.ObjectFactory;
import io.platir.service.TransactionException;
import io.platir.service.InterruptionException;
import io.platir.service.PlatirClient;
import io.platir.service.Transaction;
import io.platir.service.TransactionContext;
import io.platir.service.DataQueryException;
import io.platir.service.Queries;

/**
 * Error code explanation:
 * <ul>
 * <li>5001: A removed strateggy can't be interrupted.</li>
 * <li>5002: Cannot update strategy state.</li>
 * </ul>
 *
 * @author Chen Hongbao
 *
 */
class PlatirClientImpl extends PlatirInfoClientImpl implements PlatirClient {

    private final TransactionQueue transactionQueue;
    private final AtomicInteger transactionIdCounter = new AtomicInteger(0);
    private final AtomicBoolean isInterrupted = new AtomicBoolean(false);
    private final DateTimeFormatter transactionIdFormat = DateTimeFormatter.ofPattern("yyyyMMdd");

    PlatirClientImpl(StrategyContextImpl strategyContext, TransactionQueue trQueue, MarketRouter mkRouter, Queries queries) {
        super(strategyContext, mkRouter, queries);
        transactionQueue = trQueue;
    }

    @Override
    public TransactionContext open(String instrumentId, String direction, Double price, Integer volume) throws TransactionException {
        checkTransactionParams(instrumentId, direction, price, volume);
        return push(instrumentId, "open", direction, price, volume);
    }

    private Transaction createTransaction(String strategyId, String instrumentId, String offset, String direction, Double price, Integer volume) {
        /* Don't set state and message here. Only change the values in router daemon.*/
        var trans = ObjectFactory.newTransaction();
        trans.setTransactionId(nextTransactionId());
        trans.setStrategyId(strategyId);
        trans.setInstrumentId(instrumentId);
        trans.setPrice(price);
        trans.setVolume(volume);
        trans.setOffset(offset);
        trans.setDirection(direction);
        trans.setTradingDay(getTradingDay());
        trans.setUpdateTime(Utils.datetime());
        return trans;
    }

    private String nextTransactionId() {
        /* yyyyMMdd + <4-digits> like 200808120012 */
        return LocalDate.now().format(transactionIdFormat) + String.format("%4d", transactionIdCounter.incrementAndGet());
    }

    private void checkTransactionParams(String instrumentId, String direction, Double price, Integer volume) throws TransactionException {
        if (isInterrupted.get()) {
            throw new TransactionException("Transaction has been interruped.");
        }
        if (instrumentId == null || instrumentId.isBlank()) {
            throw new TransactionException("Invalid instrument ID(\"" + instrumentId + "\").");
        }
        if (direction == null || (direction.compareToIgnoreCase("buy") != 0 && direction.compareToIgnoreCase("sell") != 0)) {
            throw new TransactionException("Invalid direction(\"" + direction + "\").");
        }
        if (volume <= 0) {
            throw new TransactionException("Invalid volume(" + volume + ").");
        }
        if (price <= 0) {
            throw new TransactionException("Invalid price(" + price + ").");
        }
    }

    @Override
    public TransactionContext close(String instrumentId, String direction, Double price, Integer volume) throws TransactionException {
        checkTransactionParams(instrumentId, direction, price, volume);
        return push(instrumentId, "close", direction, price, volume);
    }

    private TransactionContext push(String instrumentId, String offset, String direction, Double price, Integer volume) throws TransactionException {
        var transaction = createTransaction(getStrategyId(), instrumentId, offset, direction, price, volume);
        var transactionContext = new TransactionContextImpl(transaction, getStrategyContext());
        /* strategy context has the transaction context. */
        getStrategyContext().addTransactionContext(transactionContext);
        try {
            /* save transaction to data source */
            getStrategyContext().getPlatirClientImpl().queries().insert(transaction);
            /* send the order and update trades into TransactionContext. */
            transactionQueue.push(transactionContext);
        } catch (DataQueryException exception) {
            throw new TransactionException("Can't insert or update transaction(" + transaction.getTransactionId() + ") in data source: " + exception.getMessage(), exception);
        }
        return transactionContext;
    }

    void interrupt(boolean interrupted) throws InterruptionException {
        var state = getStrategyProfile().getState();
        if (state.compareToIgnoreCase("removed") == 0) {
            throw new InterruptionException("Can't interrupt a removed strategy.");
        }
        if (interrupted) {
            getStrategyProfile().setState("interrupted");
        } else {
            getStrategyProfile().setState("running");
        }
        try {
            queries().update(getStrategyProfile());
        } catch (DataQueryException exception) {
            throw new InterruptionException("Can't update strategy state(" + getStrategyProfile().getState() + "): " + exception.getMessage(), exception);
        }
        isInterrupted.set(interrupted);
    }

}
