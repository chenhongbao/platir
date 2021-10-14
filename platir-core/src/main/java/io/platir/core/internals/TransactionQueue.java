package io.platir.core.internals;

import java.sql.SQLException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import io.platir.core.PlatirSystem;
import io.platir.service.Contract;
import io.platir.service.Notice;
import io.platir.service.Order;
import io.platir.service.RiskNotice;
import io.platir.service.Tick;
import io.platir.service.Transaction;
import io.platir.service.api.RiskAssess;
import io.platir.service.api.TradeAdaptor;

/**
 * Error code explanation:
 * <ul>
 * <li>1001: Available money is zero or below zero.
 * <li>1002: Missing instrument information.
 * <li>1003: Not enough available money to open.
 * <li>1004: Not enough position to close.
 * <li>1005: Risk assess callback throws exception.
 * </ul>
 *
 * @author Chen Hongbao
 * @since 1.0.0
 */
class TransactionQueue implements Runnable {

    private final RiskAssess rsk;
    private final TradeAdaptor tr;
    private final TradeListenerContexts lis;
    private final AtomicInteger increId = new AtomicInteger(0);
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
        increId.set(0);
        lis.clearContexts();
    }

    int countTransactionRunning(StrategyContextImpl strategy) {
        int count = 0;
        count += queueing.stream().mapToInt(t -> t.getStrategyContext() == strategy ? 1 : 0).sum();
        count += pending.stream().mapToInt(t -> t.getStrategyContext() == strategy ? 1 : 0).sum();
        return count;
    }

    void push(TransactionContextImpl ctx) throws SQLException {
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
                } catch (SQLException e) {
                    PlatirSystem.err.write("Can't update transaction(" + t.getTransactionId() + ") state("
                            + t.getState() + "): " + e.getMessage(), e);
                }
                /* Set trigger tick. */
                ctx.setTriggerTick(tick);
                if (!queueing.offer(ctx)) {
                    /*
					 * if it can't offer transaction to be executed, don't check more transaction.
                     */
                    PlatirSystem.err.write("Transaction queueing queue is full.");
                    break;
                }
            }
        }
    }

    private Set<Contract> opening(String orderId, PlatirQueryClientImpl client, Transaction transaction) {
        /*
         * Add contracts for opening. The opening margin and commission are computed
         * through the opening contracts, so just add opening contracts and account will
         * be changed.
         */
        var r = new HashSet<Contract>();
        var uid = client.getStrategyProfile().getUserId();
        for (int i = 0; i < transaction.getVolume(); ++i) {
            var c = new Contract();
            /*
             * Contract ID = <order-id>.<some-digits>
             */
            c.setContractId(orderId + "." + Integer.toString(i));
            c.setUserId(uid);
            c.setInstrumentId(transaction.getInstrumentId());
            c.setDirection(transaction.getDirection());
            c.setPrice(transaction.getPrice());
            c.setState("opening");
            c.setOpenTradingDay(client.getTradingDay());
            c.setOpenTime(PlatirSystem.datetime());
            r.add(c);

            try {
                client.queries().insert(c);
            } catch (SQLException e) {
                PlatirSystem.err.write("Can't insert user(" + c.getUserId() + ") contract(" + c.getContractId()
                        + ") opening: " + e.getMessage(), e);
            }
        }
        return r;
    }

    private Notice checkOpen(String oid, PlatirQueryClientImpl query, Transaction t) {
        var r = new Notice();
        var available = query.getAccount().getAvailable();
        if (available <= 0) {
            r.setCode(1001);
            r.setMessage("no available(" + available + ") for opening");
            return r;
        }
        var instrument = query.getInstrument(t.getInstrumentId());
        if (instrument == null) {
            r.setCode(1002);
            r.setMessage("no instrument information for " + t.getInstrumentId());
            return r;
        }
        var margin = SettlementFacilities.computeRatio(t.getPrice(), instrument.getMultiple(),
                instrument.getAmountMargin(), instrument.getVolumeMargin()) * t.getVolume();
        var commission = SettlementFacilities.computeRatio(t.getPrice(), instrument.getMultiple(),
                instrument.getAmountCommission(), instrument.getVolumeCommission()) * t.getVolume();
        if (available < margin + commission) {
            r.setCode(1003);
            r.setMessage("no available(" + available + ") for opening(" + (commission + margin) + ")");
            return r;
        }

        r.setCode(0);
        r.setMessage("good");
        /* Lock contracts for opening and return those contracts. */
        r.setObject(opening(oid, query, t));
        return r;
    }

    private OrderContextImpl createOrderContext(String orderId, String transactionId, String instrumentId, Double price,
            Integer volume, String direction, Collection<Contract> contracts, String offset,
            TransactionContextImpl transCtx) {
        var cli = transCtx.getStrategyContext().getPlatirClientImpl();
        var o = new Order();
        o.setOrderId(orderId);
        o.setTransactionId(transactionId);
        o.setInstrumentId(instrumentId);
        o.setPrice(price);
        o.setVolume(volume);
        o.setDirection(direction);
        o.setOffset(offset);
        o.setTradingDay(cli.getTradingDay());
        try {
            /* save order to data source */
            cli.queries().insert(o);
        } catch (SQLException e) {
            /* worker thread can't pass out the exception, just log it */
            PlatirSystem.err.write("Can't insert order(" + o.getOrderId() + ") to data source: " + e.getMessage(), e);
        }
        /* create order context. */
        var ctx = new OrderContextImpl(o, transCtx);
        ctx.lockedContracts().addAll(contracts);
        /* add order context to transaction context */
        transCtx.addOrderContext(ctx);
        return ctx;
    }

    private String getOrderId(String tid) {
        /* <transaction-id>.<some-digits> */
        return tid + "." + Integer.toString(increId.incrementAndGet());
    }

    @Override
    public void run() {
        while (!Thread.currentThread().isInterrupted()) {
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
                    saveCodeMessage(r.getCode(), r.getMessage(), ctx);
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
                            } catch (SQLException e) {
                                PlatirSystem.err.write("Can't update transaction(" + t.getTransactionId() + ") state("
                                        + t.getState() + "): " + e.getMessage(), e);
                            }
                            /* Notify the transaction has failed. */
                            ctx.awake();
                        }
                    }
                }
            } catch (InterruptedException e) {
                PlatirSystem.err.write("Transaction queue worker thread is interrupted.", e);
            } catch (DuplicatedOrderException e) {
                PlatirSystem.err.write("Duplicated order(ID): " + e.getMessage(), e);
            } catch (Throwable th) {
                PlatirSystem.err.write("Uncaught error: " + th.getMessage(), th);
            }
        }
    }

    private void saveCodeMessage(int code, String message, TransactionContextImpl ctx) {
        var r = new RiskNotice();
        var profile = ctx.getStrategyContext().getProfile();
        r.setCode(3002);
        r.setMessage(message);
        r.setLevel(5);
        r.setUserId(profile.getUserId());
        r.setStrategyId(profile.getStrategyId());
        r.setUpdateTime(PlatirSystem.datetime());
        try {
            ctx.getQueryClient().queries().insert(r);
        } catch (SQLException e) {
            PlatirSystem.err.write("Can't inert RiskNotice(" + code + ", " + message + "): " + e.getMessage(), e);
        }
    }

    private RiskNotice beforeRisk(Tick tick, TransactionContextImpl ctx) {
        try {
            return rsk.before(tick, ctx);
        } catch (Throwable th) {
            var profile = ctx.getStrategyContext().getProfile();
            var r = new RiskNotice();
            r.setCode(1005);
            r.setMessage("before(Tick, TransactionContext) throws exception");
            r.setUserId(profile.getUserId());
            r.setStrategyId(profile.getStrategyId());
            r.setUpdateTime(PlatirSystem.datetime());
            r.setLevel(5);
            return r;
        }
    }

    private void close(TransactionContextImpl ctx) throws DuplicatedOrderException {
        var t = ctx.getTransaction();
        var oid = getOrderId(t.getTransactionId());
        var client = ctx.getQueryClient();
        var r = checkClose(ctx.getQueryClient(), t.getInstrumentId(), t.getDirection(), t.getVolume());
        if (!r.isGood()) {
            t.setState("check-close;" + r.getCode());
            t.setStateMessage(r.getMessage());
            try {
                client.queries().update(t);
            } catch (SQLException e) {
                PlatirSystem.err.write("Can't update transaction(" + t.getTransactionId() + ") state(" + t.getState()
                        + "): " + e.getMessage(), e);
            }
            ctx.awake();
        } else {
            @SuppressWarnings("unchecked")
            var contracts = (Collection<Contract>) r.getObject();
            var tradingDay = client.getTradingDay();
            /* process today's contracts */
            var today = contracts.stream().filter(c -> c.getOpenTradingDay().equals(tradingDay))
                    .collect(Collectors.toSet());
            var orderCtxToday = createOrderContext(oid, t.getTransactionId(), t.getInstrumentId(), t.getPrice(),
                    t.getVolume(), t.getDirection(), today, "close-today", ctx);
            send(orderCtxToday, ctx);
            /* process history contracts */
            var history = contracts.stream().filter(c -> !today.contains(c)).collect(Collectors.toSet());
            var orderCtxHistory = createOrderContext(oid, t.getTransactionId(), t.getInstrumentId(), t.getPrice(),
                    t.getVolume(), t.getDirection(), history, "close-history", ctx);
            send(orderCtxHistory, ctx);
        }
    }

    private Notice checkClose(PlatirQueryClientImpl query, String instrumentId, String direction, Integer volume) {
        /* buy-open for sell-closed, sell-open for buy-closed */
        var r = new Notice();
        var available = query.getContracts(instrumentId).stream()
                .filter(c -> c.getDirection().compareToIgnoreCase(direction) != 0)
                .filter(c -> c.getState().compareToIgnoreCase("open") == 0).collect(Collectors.toSet());
        if (available.size() < volume) {
            r.setCode(1004);
            r.setMessage("no available contracts(" + available.size() + ") for closing(" + volume + ")");
            return r;
        }
        /*
	 * Remove extra contracts from container until it only has the contracts for
	 * closing and lock those contracts.
         */
        while (available.size() > volume) {
            var h = available.iterator().next();
            available.remove(h);
        }
        closing(available, query);

        r.setCode(0);
        r.setMessage("good");
        r.setObject(available);
        return r;
    }

    private void closing(Set<Contract> available, PlatirQueryClientImpl client) {
        available.stream().map(c -> {
            c.setState("closing");
            return c;
        }).forEachOrdered(c -> {
            try {
                client.queries().update(c);
            } catch (SQLException e) {
                PlatirSystem.err.write("Can't update user(" + c.getUserId() + ") + contract(" + c.getContractId()
                        + ") state(" + c.getState() + "): " + e.getMessage(), e);
            }
        });
    }

    private void open(TransactionContextImpl ctx) throws DuplicatedOrderException {
        var t = ctx.getTransaction();
        var oid = getOrderId(t.getTransactionId());
        var client = ctx.getQueryClient();
        /* Check resource. */
        var r = checkOpen(oid, client, t);
        if (!r.isGood()) {
            t.setState("check-open;" + r.getCode());
            t.setStateMessage(r.getMessage());
            try {
                client.queries().update(t);
            } catch (SQLException e) {
                PlatirSystem.err.write("Can't update transaction(" + t.getTransactionId() + ") state(" + t.getState()
                        + "): " + e.getMessage(), e);
            }
            /* notify joiner the transaction fails. */
            ctx.awake();
        } else {
            /* Lock resource for opening. */
            @SuppressWarnings("unchecked")
            var contracts = (Collection<Contract>) r.getObject();
            var orderCtx = createOrderContext(oid, t.getTransactionId(), t.getInstrumentId(), t.getPrice(),
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
            /* Update state. */
            t.setState("pending;" + ro.getCode());
            t.setStateMessage(ro.getMessage());
            try {
                client.queries().update(t);
            } catch (SQLException e) {
                PlatirSystem.err.write("Can't update transaction(" + t.getTransactionId() + ") state(" + t.getState()
                        + "): " + e.getMessage(), e);
            }
            /*
             * Put order context to pending list of the transaction, and put transaction to
             * queue's pending list.
             */
            ctx.pendingOrder().add(orderCtx);
            pending.add(ctx);
        } else {
            t.setState("running");
            t.setStateMessage("order submitted");
            try {
                client.queries().update(t);
            } catch (SQLException e) {
                PlatirSystem.err.write("Can't update transaction(" + t.getTransactionId() + ") state(" + t.getState()
                        + "): " + e.getMessage(), e);
            }
        }
        return ro;
    }

}
