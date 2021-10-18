package io.platir.core.internals;

import java.util.Collection;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import io.platir.core.internals.persistence.object.ObjectFactory;
import io.platir.service.Contract;
import io.platir.service.Notice;
import io.platir.service.RiskNotice;
import io.platir.service.Tick;
import io.platir.service.api.DataQueryException;
import io.platir.service.api.RiskAssess;
import io.platir.service.api.TradeAdaptor;

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

    private final RiskAssess rsk;
    private final TradeAdaptor tr;
    private final TradeListenerContexts lis;

    private final BlockingQueue<TransactionContextImpl> queueing = new LinkedBlockingQueue<>();
    private final Set<TransactionContextImpl> pending = new ConcurrentSkipListSet<>();

    TransactionQueue(TradeAdaptor trader, RiskAssess risk) {
        tr = trader;
        rsk = risk;
        lis = new TradeListenerContexts(rsk);
        tr.setListener(lis);
    }

    void settle() {
        pending.clear();
        queueing.clear();
        lis.clearContexts();
    }

    int countTransactionRunning(StrategyContextImpl strategy) {
        int count = 0;
        count += queueing.stream().mapToInt(t -> t.getStrategyContext() == strategy ? 1 : 0).sum();
        count += pending.stream().mapToInt(t -> t.getStrategyContext() == strategy ? 1 : 0).sum();
        count += lis.countStrategyRunning(strategy);
        return count;
    }

    void push(TransactionContextImpl ctx) throws DataQueryException {
        var t = ctx.getTransaction();
        /* Update states. */
        t.setState("pending");
        t.setStateMessage("never enqueued");
        /* Initialize adding transaction to data source */
        ctx.getQueryClient().queries().update(t);
        pending.add(ctx);
    }

    void awake(Tick tick) {
        var id = tick.getInstrumentId();
        var it = pending.iterator();
        while (it.hasNext()) {
            var ctx = it.next();
            var t = ctx.getTransaction();
            if (t.getInstrumentId().compareTo(id) == 0) {
                it.remove();
                /* Change state. */
                t.setState("queueing");
                t.setStateMessage("tick triggers queueing");
                try {
                    ctx.getQueryClient().queries().update(t);
                } catch (DataQueryException e) {
                    Utils.err.write("Can't update transaction(" + t.getTransactionId() + ") state("
                            + t.getState() + "): " + e.getMessage(), e);
                }
                /* Set trigger tick. */
                ctx.setTriggerTick(tick);
                if (!queueing.offer(ctx)) {
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
        while (!Thread.currentThread().isInterrupted() || !queueing.isEmpty()) {
            try {
                var ctx = queueing.poll(24, TimeUnit.HOURS);
                var t = ctx.getTransaction();
                /* In-front risk assessment. */
                var r = beforeRisk(ctx.getLastTriggerTick(), ctx);
                if (!r.isGood()) {
                    t.setState("in-front-risk-accessment;" + r.getCode());
                    t.setStateMessage(r.getMessage());
                    ctx.awake();
                    /* save risk notice */
                    TransactionFacilities.saveRiskNotice(r.getCode(), r.getMessage(), r.getLevel(), ctx);
                    /* notice callback */
                    TransactionFacilities.processNotice(r.getCode(), r.getMessage(), ctx);
                } else {
                    if (!ctx.pendingOrder().isEmpty()) {
                        /* the transaction has been processed but order is not completed. */
                        sendPending(ctx);
                    } else {
                        if ("open".compareToIgnoreCase(t.getOffset()) == 0) {
                            open(ctx);
                        } else if ("close".compareToIgnoreCase(t.getOffset()) == 0) {
                            close(ctx);
                        } else {
                            t.setState("invalid");
                            t.setStateMessage("invalid offset(" + t.getOffset() + ")");
                            try {
                                ctx.getQueryClient().queries().update(t);
                            } catch (DataQueryException e) {
                                Utils.err.write("Can't update transaction(" + t.getTransactionId() + ") state("
                                        + t.getState() + "): " + e.getMessage(), e);
                            }
                            /* Notify the transaction has failed. */
                            ctx.awake();
                            /* notice callback */
                            TransactionFacilities.processNotice(1006, "invalid order offset(" + t.getOffset() + ")", ctx);
                        }
                    }
                }
            } catch (InterruptedException e) {
                Utils.err.write("Transaction queue worker thread is interrupted.", e);
            } catch (DuplicatedOrderException e) {
                Utils.err.write("Duplicated order(ID): " + e.getMessage(), e);
            } catch (Throwable th) {
                Utils.err.write("Uncaught error: " + th.getMessage(), th);
            }
        }
    }

    private RiskNotice beforeRisk(Tick tick, TransactionContextImpl ctx) {
        try {
            return rsk.before(tick, ctx);
        } catch (Throwable th) {
            Utils.err.write("Risk assess after() throws exception: " + th.getMessage(), th);
            var r = ObjectFactory.newRiskNotice();
            r.setCode(1005);
            r.setMessage("before(Tick, TransactionContext) throws exception");
            r.setLevel(RiskNotice.ERROR);
            return r;
        }
    }

    private void close(TransactionContextImpl ctx) throws DuplicatedOrderException {
        var t = ctx.getTransaction();
        var oid = TransactionFacilities.getOrderId(t.getTransactionId());
        var client = ctx.getQueryClient();
        var r = TransactionFacilities.checkClose(ctx.getQueryClient(), t.getInstrumentId(), t.getDirection(), t.getVolume());
        if (!r.isGood()) {
            t.setState("check-close;" + r.getCode());
            t.setStateMessage(r.getMessage());
            try {
                client.queries().update(t);
            } catch (DataQueryException e) {
                Utils.err.write("Can't update transaction(" + t.getTransactionId() + ") state(" + t.getState()
                        + "): " + e.getMessage(), e);
            }
            ctx.awake();
            /* notice callback */
            TransactionFacilities.processNotice(r.getCode(), r.getMessage(), ctx);
        } else {
            @SuppressWarnings("unchecked")
            var contracts = (Collection<Contract>) r.getObject();
            var tradingDay = client.getTradingDay();
            /* process today's contracts */
            var today = contracts.stream().filter(c -> c.getOpenTradingDay().equals(tradingDay))
                    .collect(Collectors.toSet());
            var orderCtxToday = TransactionFacilities.createOrderContext(oid, t.getTransactionId(), t.getInstrumentId(), t.getPrice(),
                    t.getVolume(), t.getDirection(), today, "close-today", ctx);
            send(orderCtxToday, ctx);
            /* process history contracts */
            var history = contracts.stream().filter(c -> !today.contains(c)).collect(Collectors.toSet());
            var orderCtxHistory = TransactionFacilities.createOrderContext(oid, t.getTransactionId(), t.getInstrumentId(), t.getPrice(),
                    t.getVolume(), t.getDirection(), history, "close-history", ctx);
            send(orderCtxHistory, ctx);
        }
    }

    private void open(TransactionContextImpl ctx) throws DuplicatedOrderException {
        var t = ctx.getTransaction();
        var oid = TransactionFacilities.getOrderId(t.getTransactionId());
        var client = ctx.getQueryClient();
        /* Check resource. */
        var r = TransactionFacilities.checkOpen(oid, client, t);
        if (!r.isGood()) {
            t.setState("check-open;" + r.getCode());
            t.setStateMessage(r.getMessage());
            try {
                client.queries().update(t);
            } catch (DataQueryException e) {
                Utils.err.write("Can't update transaction(" + t.getTransactionId() + ") state(" + t.getState()
                        + "): " + e.getMessage(), e);
            }
            /* notify joiner the transaction fails. */
            ctx.awake();
            /* notice callback */
            TransactionFacilities.processNotice(r.getCode(), r.getMessage(), ctx);
        } else {
            /* Lock resource for opening. */
            @SuppressWarnings("unchecked")
            var contracts = (Collection<Contract>) r.getObject();
            var orderCtx = TransactionFacilities.createOrderContext(oid, t.getTransactionId(), t.getInstrumentId(), t.getPrice(),
                    t.getVolume(), t.getDirection(), contracts, "open", ctx);
            send(orderCtx, ctx);
        }
    }

    private void sendPending(TransactionContextImpl ctx) throws DuplicatedOrderException {
        var it = ctx.pendingOrder().iterator();
        while (it.hasNext()) {
            var orderCtx = it.next();
            it.remove();
            /* send order until error. */
            if (!send(orderCtx, ctx).isGood()) {
                break;
            }
        }
    }

    private Notice send(OrderContextImpl orderCtx, TransactionContextImpl ctx) throws DuplicatedOrderException {
        /*
         * Precondition: order context is not on transaction's pending order list.
         * 
         * If order is successful, map order to its locked contracts, or add order
         * context to pending list of the transaction, and put transaction to queue's
         * pending list.
         */
        var t = ctx.getTransaction();
        var client = ctx.getQueryClient();
        var order = orderCtx.getOrder();
        lis.register(orderCtx, ctx);
        tr.require(order.getOrderId(), order.getInstrumentId(), order.getOffset(), order.getDirection(),
                order.getPrice(), order.getVolume());
        /* Wait for the first response telling if the order is accepted. */
        var ro = lis.waitResponse(order.getOrderId());
        if (!ro.isGood()) {
            if (ro.getCode() == 10001) {
                /* market is not open, wait until it opens */
                t.setState("send-pending;" + ro.getCode());
                t.setStateMessage(ro.getMessage());
                try {
                    client.queries().update(t);
                } catch (DataQueryException e) {
                    Utils.err.write("Can't update transaction(" + t.getTransactionId() + ") state(" + t.getState()
                            + "): " + e.getMessage(), e);
                }
                /*
                 * Put order context to pending list of the transaction, and put transaction to
                 * queue's pending list.
                 */
                ctx.pendingOrder().add(orderCtx);
                if (!pending.contains(ctx)) {
                    /*
                     * Call send() more than once in a batch, it may be added to pending 
                     * list for more then once, Need to check if it has been added. 
                     */
                    pending.add(ctx);
                }
            } else {
                /* can't fill order because it is invalid or account is insufficient */
                t.setState("send-aborted;" + ro.getCode());
                t.setStateMessage(ro.getMessage());
                try {
                    client.queries().update(t);
                } catch (DataQueryException e) {
                    Utils.err.write("Can't update transaction(" + t.getTransactionId() + ") state(" + t.getState()
                            + "): " + e.getMessage(), e);
                }
                /* notify joiner the transaction fails. */
                ctx.awake();
            }
        } else {
            t.setState("send-running");
            t.setStateMessage("order submitted");
            try {
                client.queries().update(t);
            } catch (DataQueryException e) {
                Utils.err.write("Can't update transaction(" + t.getTransactionId() + ") state(" + t.getState()
                        + "): " + e.getMessage(), e);
            }
        }
        /* notice callback */
        TransactionFacilities.processNotice(ro.getCode(), ro.getMessage(), ctx);
        return ro;
    }

}
