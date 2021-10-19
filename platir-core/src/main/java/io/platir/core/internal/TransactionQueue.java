package io.platir.core.internal;

import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import io.platir.core.internal.objects.ObjectFactory;
import io.platir.service.Notice;
import io.platir.service.RiskNotice;
import io.platir.service.Tick;
import io.platir.service.DataQueryException;
import io.platir.service.api.TradeAdaptor;
import io.platir.service.api.RiskManager;

/**
 * Error code explanation:
 * <ul>
 * <li>1001: Available money is zero or below zero.</li>
 * <li>1002: Missing instrument information.</li>
 * <li>1003: Not enough available money to open.</li>
 * <li>1004: Not enough position to close.</li>
 * <li>1005: Risk assess callback throws exception.</li>
 * <li>1006: Invalid order offset.</li>
 * </ul>
 *
 * @author Chen Hongbao
 * @since 1.0.0
 */
class TransactionQueue implements Runnable {

    private final RiskManager riskManager;
    private final TradeAdaptor tradeAdaptor;
    private final TradeListenerContexts tradeListener;

    private final BlockingQueue<TransactionContextImpl> executingTransactions = new LinkedBlockingQueue<>();
    private final Set<TransactionContextImpl> pendingTransactions = new ConcurrentSkipListSet<>();

    TransactionQueue(TradeAdaptor tradeAdaptor, RiskManager riskManager) {
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

    int countTransactionRunning(StrategyContextImpl strategy) {
        int count = 0;
        count += executingTransactions.stream().mapToInt(t -> t.getStrategyContext() == strategy ? 1 : 0).sum();
        count += pendingTransactions.stream().mapToInt(t -> t.getStrategyContext() == strategy ? 1 : 0).sum();
        count += tradeListener.countStrategyRunning(strategy);
        return count;
    }

    void push(TransactionContextImpl transactionContext) throws DataQueryException {
        var transaction = transactionContext.getTransaction();
        /* Update states. */
        transaction.setState("pending");
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
                pendingTransaction.setState("queueing");
                pendingTransaction.setStateMessage("tick triggers queueing");
                try {
                    pending.getQueryClient().queries().update(pendingTransaction);
                } catch (DataQueryException exception) {
                    Utils.err.write("Can't update transaction(" + pendingTransaction.getTransactionId() + ") state(" + pendingTransaction.getState() + "): " + exception.getMessage(), exception);
                }
                /* Set trigger tick. */
                pending.setTriggerTick(tick);
                if (!executingTransactions.offer(pending)) {
                    /*
                     * if it can't offer transaction to be executed, don't check more transaction.
                     */
                    Utils.err.write("Transaction queueing queue is full.");
                    break;
                }
            }
        }
    }

    @Override
    public void run() {
        while (!Thread.currentThread().isInterrupted() || !executingTransactions.isEmpty()) {
            try {
                var executingContext = executingTransactions.poll(24, TimeUnit.HOURS);
                var executingTransaction = executingContext.getTransaction();
                /* In-front risk assessment. */
                var riskNotice = beforeRisk(executingContext.getLastTriggerTick(), executingContext);
                if (!riskNotice.isGood()) {
                    executingTransaction.setState("in-front-risk-accessment;" + riskNotice.getCode());
                    executingTransaction.setStateMessage(riskNotice.getMessage());
                    executingContext.awake();
                    /* save risk notice */
                    TransactionFacilities.saveRiskNotice(riskNotice.getCode(), riskNotice.getMessage(), riskNotice.getLevel(), executingContext);
                    /* notice callback */
                    TransactionFacilities.processNotice(riskNotice.getCode(), riskNotice.getMessage(), executingContext);
                } else {
                    if (!executingContext.pendingOrder().isEmpty()) {
                        /* the transaction has been processed but order is not completed. */
                        sendPending(executingContext);
                    } else {
                        if ("open".compareToIgnoreCase(executingTransaction.getOffset()) == 0) {
                            open(executingContext);
                        } else if ("close".compareToIgnoreCase(executingTransaction.getOffset()) == 0) {
                            close(executingContext);
                        } else {
                            executingTransaction.setState("invalid");
                            executingTransaction.setStateMessage("invalid offset(" + executingTransaction.getOffset() + ")");
                            try {
                                executingContext.getQueryClient().queries().update(executingTransaction);
                            } catch (DataQueryException e) {
                                Utils.err.write("Can't update transaction(" + executingTransaction.getTransactionId() + ") state("
                                        + executingTransaction.getState() + "): " + e.getMessage(), e);
                            }
                            /* Notify the transaction has failed. */
                            executingContext.awake();
                            /* notice callback */
                            TransactionFacilities.processNotice(1006, "invalid order offset(" + executingTransaction.getOffset() + ")", executingContext);
                        }
                    }
                }
            } catch (InterruptedException exception) {
                Utils.err.write("Transaction queue worker thread is interrupted.", exception);
            } catch (DuplicatedOrderException exception) {
                Utils.err.write("Duplicated order(ID): " + exception.getMessage(), exception);
            } catch (Throwable throwable) {
                Utils.err.write("Uncaught error: " + throwable.getMessage(), throwable);
            }
        }
    }

    private RiskNotice beforeRisk(Tick tick, TransactionContextImpl transactionContext) {
        try {
            return riskManager.before(tick, transactionContext);
        } catch (Throwable throwable) {
            Utils.err.write("Risk assess after() throws exception: " + throwable.getMessage(), throwable);
            var riskNotice = ObjectFactory.newRiskNotice();
            riskNotice.setCode(1005);
            riskNotice.setMessage("before(Tick, TransactionContext) throws exception");
            riskNotice.setLevel(RiskNotice.ERROR);
            return riskNotice;
        }
    }

    private void close(TransactionContextImpl transactionContext) throws DuplicatedOrderException {
        var transaction = transactionContext.getTransaction();
        var newOrderId = TransactionFacilities.getOrderId(transaction.getTransactionId());
        var queryClient = transactionContext.getQueryClient();
        var checkReturn = TransactionFacilities.checkClose(transactionContext.getQueryClient(), transaction.getInstrumentId(), transaction.getDirection(), transaction.getVolume());
        if (!checkReturn.getNotice().isGood()) {
            transaction.setState("check-close;" + checkReturn.getNotice().getCode());
            transaction.setStateMessage(checkReturn.getNotice().getMessage());
            try {
                queryClient.queries().update(transaction);
            } catch (DataQueryException e) {
                Utils.err.write("Can't update transaction(" + transaction.getTransactionId() + ") state(" + transaction.getState()
                        + "): " + e.getMessage(), e);
            }
            transactionContext.awake();
            /* notice callback */
            TransactionFacilities.processNotice(checkReturn.getNotice().getCode(), checkReturn.getNotice().getMessage(), transactionContext);
        } else {
            @SuppressWarnings("unchecked")
            var tradingDay = queryClient.getTradingDay();
            /* process today's contracts */
            var today = checkReturn.getContracts().stream().filter(c -> c.getOpenTradingDay().equals(tradingDay))
                    .collect(Collectors.toSet());
            var orderCtxToday = TransactionFacilities.createOrderContext(newOrderId, transaction.getTransactionId(), transaction.getInstrumentId(), transaction.getPrice(),
                    transaction.getVolume(), transaction.getDirection(), today, "close-today", transactionContext);
            send(orderCtxToday, transactionContext);
            /* process history contracts */
            var history = checkReturn.getContracts().stream().filter(c -> !today.contains(c)).collect(Collectors.toSet());
            var orderCtxHistory = TransactionFacilities.createOrderContext(newOrderId, transaction.getTransactionId(), transaction.getInstrumentId(), transaction.getPrice(),
                    transaction.getVolume(), transaction.getDirection(), history, "close-history", transactionContext);
            send(orderCtxHistory, transactionContext);
        }
    }

    private void open(TransactionContextImpl transactionContext) throws DuplicatedOrderException {
        var transaction = transactionContext.getTransaction();
        var newOrderId = TransactionFacilities.getOrderId(transaction.getTransactionId());
        var queryClient = transactionContext.getQueryClient();
        /* Check resource. */
        var checkReturn = TransactionFacilities.checkOpen(newOrderId, queryClient, transaction);
        if (!checkReturn.getNotice().isGood()) {
            transaction.setState("check-open;" + checkReturn.getNotice().getCode());
            transaction.setStateMessage(checkReturn.getNotice().getMessage());
            try {
                queryClient.queries().update(transaction);
            } catch (DataQueryException e) {
                Utils.err.write("Can't update transaction(" + transaction.getTransactionId() + ") state(" + transaction.getState()
                        + "): " + e.getMessage(), e);
            }
            /* notify joiner the transaction fails. */
            transactionContext.awake();
            /* notice callback */
            TransactionFacilities.processNotice(checkReturn.getNotice().getCode(), checkReturn.getNotice().getMessage(), transactionContext);
        } else {
            /* Lock resource for opening. */
            @SuppressWarnings("unchecked")
            var orderCtx = TransactionFacilities.createOrderContext(newOrderId, transaction.getTransactionId(), transaction.getInstrumentId(), transaction.getPrice(),
                    transaction.getVolume(), transaction.getDirection(), checkReturn.getContracts(), "open", transactionContext);
            send(orderCtx, transactionContext);
        }
    }

    private void sendPending(TransactionContextImpl transactionContext) throws DuplicatedOrderException {
        var pendingIterator = transactionContext.pendingOrder().iterator();
        while (pendingIterator.hasNext()) {
            var pendingOrder = pendingIterator.next();
            pendingIterator.remove();
            /* send order until error. */
            if (!send(pendingOrder, transactionContext).isGood()) {
                break;
            }
        }
    }

    private Notice send(OrderContextImpl orderContext, TransactionContextImpl transactionContext) throws DuplicatedOrderException {
        /*
         * Precondition: order context is not on transaction's pending order list.
         * 
         * If order is successful, map order to its locked contracts, or add order
         * context to pending list of the transaction, and put transaction to queue's
         * pending list.
         */
        var transaction = transactionContext.getTransaction();
        var queryClient = transactionContext.getQueryClient();
        var order = orderContext.getOrder();
        tradeListener.register(orderContext, transactionContext);
        tradeAdaptor.require(order.getOrderId(), order.getInstrumentId(), order.getOffset(), order.getDirection(), order.getPrice(), order.getVolume());
        /* Wait for the first response telling if the order is accepted. */
        var responseNotice = tradeListener.waitResponse(order.getOrderId());
        if (!responseNotice.isGood()) {
            if (responseNotice.getCode() == 10001) {
                /* market is not open, wait until it opens */
                transaction.setState("send-pending;" + responseNotice.getCode());
                transaction.setStateMessage(responseNotice.getMessage());
                try {
                    queryClient.queries().update(transaction);
                } catch (DataQueryException exception) {
                    Utils.err.write("Can't update transaction(" + transaction.getTransactionId() + ") state(" + transaction.getState() + "): " + exception.getMessage(), exception);
                }
                /*
                 * Put order context to pending list of the transaction, and put transaction to
                 * queue's pending list.
                 */
                transactionContext.pendingOrder().add(orderContext);
                if (!pendingTransactions.contains(transactionContext)) {
                    /*
                     * Call send() more than once in a batch, it may be added to pending 
                     * list for more then once, Need to check if it has been added. 
                     */
                    pendingTransactions.add(transactionContext);
                }
            } else {
                /* can't fill order because it is invalid or account is insufficient */
                transaction.setState("send-aborted;" + responseNotice.getCode());
                transaction.setStateMessage(responseNotice.getMessage());
                try {
                    queryClient.queries().update(transaction);
                } catch (DataQueryException exception) {
                    Utils.err.write("Can't update transaction(" + transaction.getTransactionId() + ") state(" + transaction.getState() + "): " + exception.getMessage(), exception);
                }
                /* notify joiner the transaction fails. */
                transactionContext.awake();
            }
        } else {
            transaction.setState("send-running");
            transaction.setStateMessage("order submitted");
            try {
                queryClient.queries().update(transaction);
            } catch (DataQueryException exception) {
                Utils.err.write("Can't update transaction(" + transaction.getTransactionId() + ") state(" + transaction.getState() + "): " + exception.getMessage(), exception);
            }
        }
        /* notice callback */
        TransactionFacilities.processNotice(responseNotice.getCode(), responseNotice.getMessage(), transactionContext);
        return responseNotice;
    }

}
