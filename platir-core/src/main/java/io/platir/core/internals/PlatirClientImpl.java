package io.platir.core.internals;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import io.platir.core.internals.persistence.object.ObjectFactory;
import io.platir.service.TransactionException;
import io.platir.service.InterruptionException;
import io.platir.service.PlatirClient;
import io.platir.service.Transaction;
import io.platir.service.TransactionContext;
import io.platir.service.api.DataQueryException;
import io.platir.service.api.Queries;

/**
 * Error code explanation:
 * <ul>
 * <li>5001: A removed strateggy can't be interrupted.
 * <li>5002: Cannot update strategy state.
 * </ul>
 *
 * @author Chen Hongbao
 *
 */
class PlatirClientImpl extends PlatirInfoClientImpl implements PlatirClient {

    private final TransactionQueue tr;
    private final AtomicInteger increId = new AtomicInteger(0);
    private final AtomicBoolean isInterrupted = new AtomicBoolean(false);
    private final DateTimeFormatter transIdFmt = DateTimeFormatter.ofPattern("yyyyMMdd");

    PlatirClientImpl(StrategyContextImpl strategyContext, TransactionQueue trQueue, MarketRouter mkRouter,
            Queries queries) {
        super(strategyContext, mkRouter, queries);
        tr = trQueue;
    }

    @Override
    public TransactionContext open(String instrumentId, String direction, Double price, Integer volume)
            throws TransactionException {
        checkTransactionParams(instrumentId, direction, price, volume);
        return push(instrumentId, "open", direction, price, volume);
    }

    private Transaction createTransaction(String strategyId, String instrumentId, String offset, String direction,
            Double price, Integer volume) {
        /*
         * Don't set state and message here. Only change the values in router daemon.
         */
        var trans = ObjectFactory.newTransaction();
        trans.setTransactionId(getTransId());
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

    private String getTransId() {
        /*
         * yyyyMMdd + <4-digits> 200808120012
         */
        return LocalDate.now().format(transIdFmt) + String.format("%4d", increId.incrementAndGet());
    }

    private void checkTransactionParams(String instrumentId, String direction, Double price, Integer volume)
            throws TransactionException {
        if (isInterrupted.get()) {
            throw new TransactionException("Transaction has been interruped.");
        }
        if (instrumentId == null || instrumentId.isBlank()) {
            throw new TransactionException("Invalid instrument ID(\"" + instrumentId + "\").");
        }
        if (direction == null
                || (direction.compareToIgnoreCase("buy") != 0 && direction.compareToIgnoreCase("sell") != 0)) {
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
    public TransactionContext close(String instrumentId, String direction, Double price, Integer volume)
            throws TransactionException {
        checkTransactionParams(instrumentId, direction, price, volume);
        return push(instrumentId, "close", direction, price, volume);
    }

    private TransactionContext push(String instrumentId, String offset, String direction, Double price, Integer volume)
            throws TransactionException {
        var trans = createTransaction(getStrategyId(), instrumentId, offset, direction, price, volume);
        var transCtx = new TransactionContextImpl(trans, getStrategyContext());
        /* strategy context has the transaction context. */
        getStrategyContext().addTransactionContext(transCtx);
        try {
            /* save transaction to data source */
            getStrategyContext().getPlatirClientImpl().queries().insert(trans);
            /* send the order and update trades into TransactionContext. */
            tr.push(transCtx);
        } catch (DataQueryException e) {
            throw new TransactionException("Can't insert or update transaction(" + trans.getTransactionId()
                    + ") in data source: " + e.getMessage(), e);
        }
        return transCtx;
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
        } catch (DataQueryException e) {
            throw new InterruptionException(
                    "Can't update strategy state(" + getStrategyProfile().getState() + "): " + e.getMessage(), e);
        }
        isInterrupted.set(interrupted);
    }

}
