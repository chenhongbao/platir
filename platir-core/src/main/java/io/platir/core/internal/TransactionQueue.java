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
                if (riskNotice.getCode() != ServiceConstants.CODE_OK) {
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
        var transaction = transactionContext.getTransaction();
        var newOrderId = TransactionFacilities.getOrderId(transaction.getTransactionId());
        var queryClient = transactionContext.getQueryClient();
        var checkReturn = TransactionFacilities.checkClose(transactionContext.getQueryClient(), transaction.getInstrumentId(), transaction.getDirection(), transaction.getVolume());
        if (checkReturn.getCode() != ServiceConstants.CODE_OK) {
            updateTransactionState(ServiceConstants.FLAG_TRANSACTION_CHECK_CLOSE + ";" + checkReturn.getCode(), checkReturn.getMessage(), transactionContext);
            transactionContext.awake();
            /* Trade update callback. */
            TransactionFacilities.processTradeUpdate(checkReturn.getCode(), checkReturn.getMessage(), null, transactionContext, null);
        } else {
            @SuppressWarnings("unchecked")
            var tradingDay = queryClient.getTradingDay();
            /* Process today and history contracts. */
            var todayContracts = checkReturn.getContracts().stream().filter(c -> c.getOpenTradingDay().equals(tradingDay)).collect(Collectors.toSet());
            var todayOrderContext = TransactionFacilities.createOrderContext(newOrderId, transaction.getTransactionId(), transaction.getInstrumentId(), transaction.getPrice(),
                    transaction.getVolume(), transaction.getDirection(), todayContracts, ApiConstants.FLAG_CLOSE_TODAY, transactionContext);
            var historyContracts = checkReturn.getContracts().stream().filter(c -> !todayContracts.contains(c)).collect(Collectors.toSet());
            var historyOrderContext = TransactionFacilities.createOrderContext(newOrderId, transaction.getTransactionId(), transaction.getInstrumentId(), transaction.getPrice(),
                    transaction.getVolume(), transaction.getDirection(), historyContracts, ApiConstants.FLAG_CLOSE_HISTORY, transactionContext);
            int returnCode = send(todayOrderContext, transactionContext);
            switch (returnCode) {
                case ApiConstants.CODE_MARKET_CLOSED:
                    /* Two orders are pending. */
                    transactionContext.pendingOrders().add(todayOrderContext);
                    transactionContext.pendingOrders().add(historyOrderContext);
                    pendingTransactions.add(transactionContext);
                    updateTransactionState(ServiceConstants.FLAG_TRANSACTION_PENDING, transactionContext.pendingOrders().size() + "pending orders", transactionContext);
                    TransactionFacilities.processTradeUpdate(ServiceConstants.CODE_TRANSACTION_SEND_PENDING, "send pending", null, transactionContext, null);
                    break;
                case ApiConstants.CODE_OK:
                    /* Today order is sent, continue processing history order. */
                    returnCode = send(historyOrderContext, transactionContext);
                    switch (returnCode) {
                        case ApiConstants.CODE_MARKET_CLOSED:
                            /* Only history order is pending. */
                            transactionContext.pendingOrders().add(historyOrderContext);
                            pendingTransactions.add(transactionContext);
                            updateTransactionState(ServiceConstants.FLAG_TRANSACTION_PENDING, transactionContext.pendingOrders().size() + "pending orders", transactionContext);
                            TransactionFacilities.processTradeUpdate(ServiceConstants.CODE_TRANSACTION_SEND_PENDING, "send pending", historyOrderContext, transactionContext, null);
                            break;
                        case ApiConstants.CODE_OK:
                            /* Both today order and history order are sent.*/
                            updateTransactionState(ServiceConstants.FLAG_TRANSACTION_SEND_OK, "send ok", transactionContext);
                            TransactionFacilities.processTradeUpdate(ServiceConstants.CODE_TRANSACTION_SEND_OK, "send ok", null, transactionContext, null);
                            break;
                        default:
                            /* History order fails, so only a part of transaction is sent. */
                            updateTransactionState(ServiceConstants.FLAG_TRANSACTION_SEND_PART, "send part", transactionContext);
                            TransactionFacilities.processTradeUpdate(ServiceConstants.CODE_TRANSACTION_SEND_PART, "send part", null, transactionContext, null);
                            break;
                    }
                    break;
                default:
                    /* Today order fails, also abort the history order. */
                    transactionContext.awake();
                    updateTransactionState(ServiceConstants.FLAG_TRANSACTION_SEND_ABORT, "send abort", transactionContext);
                    TransactionFacilities.processTradeUpdate(ServiceConstants.CODE_TRANSACTION_SEND_ABORT, "send abort", null, transactionContext, null);
                    break;
            }
        }
    }

    private void open(TransactionContextImpl transactionContext) throws DuplicatedOrderException {
        var transaction = transactionContext.getTransaction();
        var newOrderId = TransactionFacilities.getOrderId(transaction.getTransactionId());
        var queryClient = transactionContext.getQueryClient();
        /* Check resource. */
        var checkReturn = TransactionFacilities.checkOpen(newOrderId, queryClient, transaction);
        if (checkReturn.getCode() != ServiceConstants.CODE_OK) {
            updateTransactionState(ServiceConstants.FLAG_TRANSACTION_CHECK_OPEN + ";" + checkReturn.getCode(), checkReturn.getMessage(), transactionContext);
            /* notify joiner the transaction fails. */
            transactionContext.awake();
            /* Trade update callback. */
            TransactionFacilities.processTradeUpdate(checkReturn.getCode(), checkReturn.getMessage(), null, transactionContext, null);
        } else {
            /* Lock resource for opening. */
            @SuppressWarnings("unchecked")
            var orderContext = TransactionFacilities.createOrderContext(newOrderId, transaction.getTransactionId(), transaction.getInstrumentId(), transaction.getPrice(),
                    transaction.getVolume(), transaction.getDirection(), checkReturn.getContracts(), "open", transactionContext);
            var returnCode = send(orderContext, transactionContext);
            switch (returnCode) {
                case ApiConstants.CODE_MARKET_CLOSED:
                    /* Open order is pending. */
                    transactionContext.pendingOrders().add(orderContext);
                    pendingTransactions.add(transactionContext);
                    updateTransactionState(ServiceConstants.FLAG_TRANSACTION_PENDING, transactionContext.pendingOrders().size() + "pending orders", transactionContext);
                    TransactionFacilities.processTradeUpdate(ServiceConstants.CODE_TRANSACTION_SEND_PENDING, "send pending", orderContext, transactionContext, null);
                    break;
                case ApiConstants.CODE_OK:
                    /* Open order is sent OK. */
                    updateTransactionState(ServiceConstants.FLAG_TRANSACTION_SEND_OK, "send ok", transactionContext);
                    TransactionFacilities.processTradeUpdate(ServiceConstants.CODE_TRANSACTION_SEND_OK, "send ok", orderContext, transactionContext, null);
                    break;
                default:
                    /* Open order is aborted. */
                    transactionContext.awake();
                    updateTransactionState(ServiceConstants.FLAG_TRANSACTION_SEND_ABORT, "send abort", transactionContext);
                    TransactionFacilities.processTradeUpdate(ServiceConstants.CODE_TRANSACTION_SEND_ABORT, "send abort", orderContext, transactionContext, null);
                    break;
            }
        }
    }

    private boolean sendPending(TransactionContextImpl transactionContext) throws DuplicatedOrderException {
        var pendingAgain = new HashSet<OrderContextImpl>();
        while (!transactionContext.pendingOrders().isEmpty()) {
            var pendingOrder = transactionContext.pendingOrders().poll();
            if (pendingOrder == null) {
                /* If pending orders are polled in other places, it may be null before calling poll(). */
                break;
            }
            int returnCode = send(pendingOrder, transactionContext);
            if (returnCode == ApiConstants.CODE_MARKET_CLOSED) {
                /* Only failure for market close needs a second try. If it succeeds or fails because of invalidity, drop the order. */
                pendingAgain.add(pendingOrder);
            }
        }
        if (!pendingAgain.isEmpty()) {
            transactionContext.pendingOrders().addAll(pendingAgain);
            updateTransactionState(ServiceConstants.FLAG_TRANSACTION_PENDING, transactionContext.pendingOrders().size() + "pending orders", transactionContext);
            return false;
        } else {
            /* Pending orders are sent or aborted. */
            if (transactionContext.hasFailedOrder()) {
                /* There are some failed orders. */
                if (transactionContext.hasSuccessOrder()) {
                    /* Part transaction succeeds. */
                    updateTransactionState(ServiceConstants.FLAG_TRANSACTION_SEND_PART, "send part", transactionContext);
                    TransactionFacilities.processTradeUpdate(ServiceConstants.CODE_TRANSACTION_SEND_PART, "send part", null, transactionContext, null);
                } else {
                    /* All failed. */
                    transactionContext.awake();
                    updateTransactionState(ServiceConstants.FLAG_TRANSACTION_SEND_ABORT, "send abort", transactionContext);
                    TransactionFacilities.processTradeUpdate(ServiceConstants.CODE_TRANSACTION_SEND_ABORT, "send abort", null, transactionContext, null);
                }
            } else {
                /* No order failed, all succeeded.*/
                updateTransactionState(ServiceConstants.FLAG_TRANSACTION_SEND_OK, "send ok", transactionContext);
                TransactionFacilities.processTradeUpdate(ServiceConstants.CODE_TRANSACTION_SEND_OK, "send ok", null, transactionContext, null);
            }
            return true;
        }
    }

    private int send(OrderContextImpl orderContext, TransactionContextImpl transactionContext) throws DuplicatedOrderException {
        var order = orderContext.getOrder();
        tradeListener.register(order.getOrderId(), orderContext, transactionContext);
        var returnCode = tradeAdaptor.require(order.getOrderId(), order.getInstrumentId(), order.getOffset(), order.getDirection(), order.getPrice(), order.getVolume());
        if (returnCode != ApiConstants.CODE_OK) {
            tradeListener.unregister(order.getOrderId());
            if (returnCode != ApiConstants.CODE_MARKET_CLOSED) {
                transactionContext.addFailedOrder(orderContext);
            }
        } else {
            transactionContext.addSuccessOrder(orderContext);
        }
        /* notice callback */
        TransactionFacilities.processTradeUpdate(returnCode, Integer.toString(returnCode), orderContext, transactionContext, null);
        return returnCode;
    }

    private void updateTransactionState(String state, String stateMessage, TransactionContextImpl transactionContext) {
        var transaction = transactionContext.getTransaction();
        transaction.setState(state);
        transaction.setStateMessage(stateMessage);
        try {
            transactionContext.getQueryClient().queries().update(transaction);
        } catch (DataQueryException exception) {
            Utils.err().write("Can't update transaction(" + transaction.getTransactionId() + ") state(" + transaction.getState() + "): " + exception.getMessage(), exception);
        }
    }

}
