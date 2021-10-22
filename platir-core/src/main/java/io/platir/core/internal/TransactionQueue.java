package io.platir.core.internal;

import io.platir.service.ServiceConstants;
import io.platir.queries.Utils;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import io.platir.service.RiskNotice;
import io.platir.service.Tick;
import io.platir.service.DataQueryException;
import io.platir.service.Factory;
import io.platir.service.Transaction;
import io.platir.service.api.RiskManager;
import io.platir.service.api.ApiConstants;
import io.platir.service.api.TradeAdapter;
import java.util.HashSet;

/**
 *
 * @author Chen Hongbao
 * @since 1.0.0
 */
class TransactionQueue implements Runnable {

    private final Factory factory;
    private final RiskManager riskManager;
    private final TradeAdapter tradeAdaptor;
    private final TradeListenerContexts tradeListener;

    private final BlockingQueue<TransactionContextImpl> executingTransactions = new LinkedBlockingQueue<>();
    private final Set<TransactionContextImpl> pendingTransactions = new ConcurrentSkipListSet<>();

    TransactionQueue(TradeAdapter tradeAdaptor, RiskManager riskManager, Factory factory) {
        this.factory = factory;
        this.tradeAdaptor = tradeAdaptor;
        this.riskManager = riskManager;
        this.tradeListener = new TradeListenerContexts(riskManager);
        this.tradeAdaptor.setListener(tradeListener);
    }

    void settle() {
        pendingTransactions.clear();
        executingTransactions.clear();
        tradeListener.clearContexts();
    }

    int countPendingTransactions(StrategyContextImpl strategy) {
        return pendingTransactions.stream().mapToInt(t -> t.getStrategyContext() == strategy ? 1 : 0).sum();
    }

    int countExecutingTransactions(StrategyContextImpl strategy) {
        return executingTransactions.stream().mapToInt(t -> t.getStrategyContext() == strategy ? 1 : 0).sum();
    }

    int countTradingTransactions(StrategyContextImpl strategy) {
        return tradeListener.countStrategyRunning(strategy);
    }

    int countOnlineTransactions(StrategyContextImpl strategy) {
        return countPendingTransactions(strategy) + countExecutingTransactions(strategy) + countTradingTransactions(strategy);
    }

    void push(TransactionContextImpl transactionContext) throws DataQueryException {
        var transaction = transactionContext.getTransaction();
        /* Update states. */
        transaction.setState(ServiceConstants.FLAG_TRANSACTION_PENDING);
        transaction.setStateMessage("never enqueued");
        /* Initialize adding transaction to data source */
        transactionContext.getQueryClient().queries().update(transaction);
        pendingTransactions.add(transactionContext);
    }

    void awake(Tick tick) {
        var instrumentId = tick.getInstrumentId();
        var pendingIterator = pendingTransactions.iterator();
        while (pendingIterator.hasNext()) {
            var pending = pendingIterator.next();
            var pendingTransaction = pending.getTransaction();
            if (pendingTransaction.getInstrumentId().compareTo(instrumentId) == 0) {
                pendingIterator.remove();
                /* Change state. */
                pendingTransaction.setState(ServiceConstants.FLAG_TRANSACTION_EXECUTING);
                pendingTransaction.setStateMessage("tick triggers executiion");
                try {
                    pending.getQueryClient().queries().update(pendingTransaction);
                } catch (DataQueryException exception) {
                    Utils.err().write("Can't update transaction(" + pendingTransaction.getTransactionId() + ") state(" + pendingTransaction.getState() + "): " + exception.getMessage(), exception);
                }
                /* Set trigger tick. */
                pending.setTriggerTick(tick);
                if (!executingTransactions.offer(pending)) {
                    /* If it can't offer transaction to be executed, don't check more transaction.*/
                    Utils.err().write("Transaction queueing queue is full.");
                    break;
                }
            }
        }
    }

    @Override
    public void run() {
        while (!Thread.currentThread().isInterrupted() || !executingTransactions.isEmpty()) {
            try {
                Throwable throwable = null;
                var executingContext = executingTransactions.poll(24, TimeUnit.HOURS);
                var executingTransaction = executingContext.getTransaction();
                /* In-front risk assessment. */
                var riskNotice = beforeRisk(executingContext.getLastTriggerTick(), executingContext);
                if (riskNotice.getCode() == ServiceConstants.CODE_OK) {
                    executingTransaction.setState(ServiceConstants.FLAG_TRANSACTION_RISK_BLOCKED + ";" + riskNotice.getCode());
                    executingTransaction.setStateMessage(riskNotice.getMessage());
                    executingContext.awake();
                    /* save risk notice */
                    TransactionFacilities.saveRiskNotice(riskNotice.getCode(), riskNotice.getMessage(), riskNotice.getLevel(), executingContext);
                    /* notice callback */
                    TransactionFacilities.processTradeUpdate(riskNotice.getCode(), riskNotice.getMessage(), null, executingContext, throwable);
                } else {
                    if (!executingContext.pendingOrders().isEmpty()) {
                        /* The transaction has been processed but order is not completed. */
                        if (!sendPending(executingContext)) {
                            /* If pending orders in transaction are not completed, transaction waits again in pending list. */
                            pendingTransactions.add(executingContext);
                        }
                    } else {
                        switch (executingTransaction.getOffset()) {
                            case ApiConstants.FLAG_OPEN:
                                open(executingContext);
                                break;
                            case ApiConstants.FLAG_CLOSE:
                                close(executingContext);
                                break;
                            default:
                                executingTransaction.setState(ServiceConstants.FLAG_TRANSACTION_INVALID);
                                executingTransaction.setStateMessage("invalid offset(" + executingTransaction.getOffset() + ")");
                                try {
                                    executingContext.getQueryClient().queries().update(executingTransaction);
                                } catch (DataQueryException exception) {
                                    throwable = exception;
                                    Utils.err().write("Can't update transaction(" + executingTransaction.getTransactionId() + ") state(" + executingTransaction.getState() + "): " + exception.getMessage(), exception);
                                }
                                /* Notify the transaction has failed. */
                                executingContext.awake();
                                /* Notice callback. */
                                TransactionFacilities.processTradeUpdate(ServiceConstants.CODE_INVALID_OFFSET, "invalid order offset(" + executingTransaction.getOffset() + ")", null, executingContext, throwable);
                                break;
                        }
                    }
                }
            } catch (InterruptedException exception) {
                Utils.err().write("Transaction queue worker thread is interrupted.", exception);
            } catch (DuplicatedOrderException exception) {
                Utils.err().write("Duplicated order(ID): " + exception.getMessage(), exception);
            } catch (Throwable throwable) {
                Utils.err().write("Uncaught error: " + throwable.getMessage(), throwable);
            }
        }
    }

    private RiskNotice beforeRisk(Tick tick, TransactionContextImpl transactionContext) {
        try {
            return riskManager.before(tick, transactionContext);
        } catch (Throwable throwable) {
            Utils.err().write("Risk assess after() throws exception: " + throwable.getMessage(), throwable);
            var riskNotice = factory.newRiskNotice();
            riskNotice.setCode(ServiceConstants.CODE_OK);
            riskNotice.setMessage("before(Tick, TransactionContext) not callable");
            return riskNotice;
        }
    }

    private void close(TransactionContextImpl transactionContext) throws DuplicatedOrderException {
        Throwable throwable = null;
        var transaction = transactionContext.getTransaction();
        var newOrderId = TransactionFacilities.getOrderId(transaction.getTransactionId());
        var queryClient = transactionContext.getQueryClient();
        var checkReturn = TransactionFacilities.checkClose(transactionContext.getQueryClient(), transaction.getInstrumentId(), transaction.getDirection(), transaction.getVolume());
        if (checkReturn.getCode() != ServiceConstants.CODE_OK) {
            transaction.setState(ServiceConstants.FLAG_TRANSACTION_CHECK_CLOSE + ";" + checkReturn.getCode());
            transaction.setStateMessage(checkReturn.getMessage());
            try {
                queryClient.queries().update(transaction);
            } catch (DataQueryException exception) {
                throwable = exception;
                Utils.err().write("Can't update transaction(" + transaction.getTransactionId() + ") state(" + transaction.getState() + "): " + exception.getMessage(), exception);
            }
            transactionContext.awake();
            /* notice callback */
            TransactionFacilities.processTradeUpdate(checkReturn.getCode(), checkReturn.getMessage(), null, transactionContext, throwable);
        } else {
            @SuppressWarnings("unchecked")
            var tradingDay = queryClient.getTradingDay();
            /* process today's contracts */
            var todayContracts = checkReturn.getContracts().stream().filter(c -> c.getOpenTradingDay().equals(tradingDay)).collect(Collectors.toSet());
            var todayOrderContext = TransactionFacilities.createOrderContext(newOrderId, transaction.getTransactionId(), transaction.getInstrumentId(), transaction.getPrice(),
                    transaction.getVolume(), transaction.getDirection(), todayContracts, ApiConstants.FLAG_CLOSE_TODAY, transactionContext);
            int returnCode = send(todayOrderContext, transactionContext);
            if (returnCode == ApiConstants.CODE_OK) {
                /* process history contracts */
                var historyContracts = checkReturn.getContracts().stream().filter(c -> !todayContracts.contains(c)).collect(Collectors.toSet());
                var historyOrderContext = TransactionFacilities.createOrderContext(newOrderId, transaction.getTransactionId(), transaction.getInstrumentId(), transaction.getPrice(),
                        transaction.getVolume(), transaction.getDirection(), historyContracts, ApiConstants.FLAG_CLOSE_HISTORY, transactionContext);
                returnCode = send(historyOrderContext, transactionContext);
                if (returnCode == ApiConstants.CODE_MARKET_CLOSED) {
                    transactionContext.pendingOrders().add(historyOrderContext);
                    pendingTransactions.add(transactionContext);
                }
            } else {
                if (returnCode == ApiConstants.CODE_MARKET_CLOSED) {
                    transactionContext.pendingOrders().add(todayOrderContext);
                    pendingTransactions.add(transactionContext);
                }
            }
        }
    }

    private void open(TransactionContextImpl transactionContext) throws DuplicatedOrderException {
        Throwable throwable = null;
        var transaction = transactionContext.getTransaction();
        var newOrderId = TransactionFacilities.getOrderId(transaction.getTransactionId());
        var queryClient = transactionContext.getQueryClient();
        /* Check resource. */
        var checkReturn = TransactionFacilities.checkOpen(newOrderId, queryClient, transaction);
        if (checkReturn.getCode() != ServiceConstants.CODE_OK) {
            transaction.setState(ServiceConstants.FLAG_TRANSACTION_CHECK_OPEN + ";" + checkReturn.getCode());
            transaction.setStateMessage(checkReturn.getMessage());
            try {
                queryClient.queries().update(transaction);
            } catch (DataQueryException exception) {
                throwable = exception;
                Utils.err().write("Can't update transaction(" + transaction.getTransactionId() + ") state(" + transaction.getState() + "): " + exception.getMessage(), exception);
            }
            /* notify joiner the transaction fails. */
            transactionContext.awake();
            /* notice callback */
            TransactionFacilities.processTradeUpdate(checkReturn.getCode(), checkReturn.getMessage(), null, transactionContext, throwable);
        } else {
            /* Lock resource for opening. */
            @SuppressWarnings("unchecked")
            var orderContext = TransactionFacilities.createOrderContext(newOrderId, transaction.getTransactionId(), transaction.getInstrumentId(), transaction.getPrice(),
                    transaction.getVolume(), transaction.getDirection(), checkReturn.getContracts(), "open", transactionContext);
            var returnCode = send(orderContext, transactionContext);
            if (returnCode == ApiConstants.CODE_MARKET_CLOSED) {
                transactionContext.pendingOrders().add(orderContext);
                pendingTransactions.add(transactionContext);
            }
        }
    }

    private boolean sendPending(TransactionContextImpl transactionContext) throws DuplicatedOrderException {
        var failedOrders = new HashSet<OrderContextImpl>();
        while (!transactionContext.pendingOrders().isEmpty()) {
            var pendingOrder = transactionContext.pendingOrders().poll();
            if (pendingOrder == null) {
                /* If pending orders are polled in other places, it may be null before calling poll(). */
                break;
            }
            int returnCode = send(pendingOrder, transactionContext);
            if (returnCode == ApiConstants.CODE_MARKET_CLOSED) {
                /* Only failure for market close needs a second try. If it succeeds or fails because of invalidity, drop the order. */
                failedOrders.add(pendingOrder);
            } else if (returnCode != ApiConstants.CODE_OK) {
                /* Invalidity error. */
                TransactionFacilities.processTradeUpdate(returnCode, "fatal error", pendingOrder, transactionContext, null);
            }
        }
        if (!failedOrders.isEmpty()) {
            transactionContext.pendingOrders().addAll(failedOrders);
            return false;
        } else {
            return true;
        }
    }

    private int send(OrderContextImpl orderContext, TransactionContextImpl transactionContext) throws DuplicatedOrderException {
        Throwable throwable;
        var order = orderContext.getOrder();
        tradeListener.register(order.getOrderId(), orderContext, transactionContext);
        var returnCode = tradeAdaptor.require(order.getOrderId(), order.getInstrumentId(), order.getOffset(), order.getDirection(), order.getPrice(), order.getVolume());
        if (returnCode != ApiConstants.CODE_OK) {
            tradeListener.unregister(order.getOrderId());
            if (returnCode == ApiConstants.CODE_MARKET_CLOSED) {
                /* Market is not open, wait until it opens. */
                throwable = updateTransactionState(ServiceConstants.FLAG_TRANSACTION_SEND_PENDING + ";" + returnCode, Integer.toString(returnCode), transactionContext);
            } else {
                /* Can't fill order because it is invalid or account is insufficient. */
                throwable = updateTransactionState(ServiceConstants.FLAG_TRANSACTION_SEND_ABORT + ";" + returnCode, Integer.toString(returnCode), transactionContext);
                /* Notify joiner the transaction fails. */
                transactionContext.awake();
            }
        } else {
            throwable = updateTransactionState(ServiceConstants.FLAG_TRANSACTION_SEND_OK, "order sent ok", transactionContext);
        }
        /* notice callback */
        TransactionFacilities.processTradeUpdate(returnCode, Integer.toString(returnCode), orderContext, transactionContext, throwable);
        return returnCode;
    }

    private Throwable updateTransactionState(String state, String stateMessage, TransactionContextImpl transactionContext) {
        var transaction = transactionContext.getTransaction();
        transaction.setState(state);
        transaction.setStateMessage(stateMessage);
        try {
            transactionContext.getQueryClient().queries().update(transaction);
            return null;
        } catch (DataQueryException exception) {
            Utils.err().write("Can't update transaction(" + transaction.getTransactionId() + ") state(" + transaction.getState() + "): " + exception.getMessage(), exception);
            return exception;
        }
    }

}
