package io.platir.core.internals;

import io.platir.core.PlatirSystem;
import io.platir.core.internals.persistence.object.ObjectFactory;
import io.platir.service.Notice;
import io.platir.service.RiskNotice;
import io.platir.service.Trade;
import io.platir.service.api.RiskAssess;
import io.platir.service.api.TradeListener;
import java.sql.SQLException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Error code explanation:
 * <ul>
 * <li>3001: Trade response timeout.
 * <li>3002: Trade more than expected.
 * <li>3003: Locked contracts are less than traded contracts.
 * </ul>
 */
class TradeListenerContexts implements TradeListener {

    private final Map<String, OrderExecutionContext> ctxs = new ConcurrentHashMap<>();
    private final RiskAssess rsk;

    TradeListenerContexts(RiskAssess risk) {
        rsk = risk;
    }

    void clearContexts() {
        ctxs.clear();
    }

    void register(OrderContextImpl orderCtx, TransactionContextImpl ctx) throws DuplicatedOrderException {
        var orderId = orderCtx.getOrder().getOrderId();
        if (ctxs.containsKey(orderId)) {
            throw new DuplicatedOrderException("Order(" + orderId + ") duplicated.");
        }
        ctxs.put(orderId, new OrderExecutionContext(orderCtx, ctx));
    }

    Notice waitResponse(String orderId) {
        var ctx = ctxs.get(orderId);
        if (ctx != null) {
            return ctx.waitResponse();
        } else {
            throw new RuntimeException("Order execution context not found for order(" + orderId + ").");
        }
    }

    @Override
    public void onTrade(Trade trade) {
        var ctx = ctxs.get(trade.getOrderId());
        if (ctx != null) {
            ctx.processTrade(trade);
        } else {
            PlatirSystem.err.write("Order execution context not found for order(" + trade.getOrderId() + ").");
        }
    }

    @Override
    public void onNotice(String orderId, int code, String message) {
        var ctx = ctxs.get(orderId);
        if (ctx != null) {
            ctx.processNotice(code, message);
        } else {
            PlatirSystem.err.write("Order execution context not found for order(" + orderId + ").");
        }
    }

    private class OrderExecutionContext {

        private Notice notice;
        private OrderContextImpl oCtx;
        private TransactionContextImpl trCtx;
        private final AtomicInteger count = new AtomicInteger(0);

        /* first notice waiting facilities */
        private final Lock l = new ReentrantLock();
        private final Condition cond = l.newCondition();
        private final int timeoutSec = 5;

        OrderExecutionContext(OrderContextImpl orderCtx, TransactionContextImpl ctx) {
            oCtx = orderCtx;
            trCtx = ctx;
        }

        void processTrade(Trade trade) {
            var stg = trCtx.getStrategyContext();
            try {
                /* save trade to data source. */
                stg.getPlatirClientImpl().queries().insert(trade);
            } catch (SQLException e) {
                /* worker thread sees this exception, just log it */
                PlatirSystem.err.write("Can't insert trade(" + trade.getTradeId() + ") for transaction(" + trCtx.getTransaction() + ") and strategy(" + stg.getProfile().getStrategyId() + ") into data source: " + e.getMessage(), e);
            }
            /* add trade to order context. */
            oCtx.addTrade(trade);
            /* update contracts' states */
            updateContracts(trade);
            stg.timedOnTrade(trade);
            checkCompleted(trade.getVolume());
            /* risk assess */
            afterRisk(trade);
        }

        void processNotice(int code, String message) {
            signalResponse(code, message);
            timedOnNotice(code, message);
        }

        void signalResponse(int code, String message) {
            if (notice != null) {
                return;
            }
            l.lock();
            try {
                if (notice == null) {
                    notice = ObjectFactory.newNotice();
                    notice.setCode(code);
                    notice.setMessage(message);
                    cond.signalAll();
                }
            } finally {
                l.unlock();
            }
        }

        Notice waitResponse() {
            if (notice != null) {
                return notice;
            }
            l.lock();
            try {
                if (notice == null) {
                    cond.await(timeoutSec, TimeUnit.SECONDS);
                }
            } catch (InterruptedException e) {
                notice = ObjectFactory.newNotice();
                notice.setCode(3001);
                notice.setMessage("response timeout");
                notice.setObject(e);
            } finally {
                l.unlock();
            }
            return notice;
        }

        private void afterRisk(Trade trade) {
            try {
                var r = rsk.after(trade, trCtx);
                if (!r.isGood()) {
                    saveCodeMessage0(r.getCode(), r.getMessage());
                }
            } catch (Throwable th) {
                PlatirSystem.err.write("Risk assess after() throws exception: " + th.getMessage(), th);
            }
        }

        private void updateContracts(Trade trade) {
            var stg = trCtx.getStrategyContext();
            int updateCount = 0;
            var it = oCtx.lockedContracts().iterator();
            while (++updateCount <= trade.getVolume() && it.hasNext()) {
                var c = it.next();
                var prevState = c.getState();
                if (c.getState().compareToIgnoreCase("opening") == 0) {
                    /* Update open price because the real traded price may be different. */
                    c.setState("open");
                    c.setPrice(trade.getPrice());
                    c.setOpenTime(PlatirSystem.datetime());
                    c.setOpenTradingDay(stg.getPlatirClientImpl().getTradingDay());
                } else if (c.getState().compareToIgnoreCase("closing") == 0) {
                    /* don't forget the close price here */
                    c.setState("closed");
                    c.setClosePrice(trade.getPrice());
                } else {
                    PlatirSystem.err.write("Incorrect contract state(" + c.getState() + "/" + c.getContractId() + ") before completing trade.");
                    continue;
                }
                try {
                    stg.getPlatirClientImpl().queries().update(c);
                } catch (SQLException e) {
                    PlatirSystem.err.write("Fail updating user(" + c.getUserId() + ") contract(" + c.getContractId() + ") state(" + c.getState() + ").", e);
                    /* roll back state */
                    c.setState(prevState);
                    continue;
                }
                it.remove();
            }
            if (updateCount <= trade.getVolume()) {
                var msg = "Insufficent(" + updateCount + "<" + trade.getVolume() + ") locked contracts.";
                PlatirSystem.err.write(msg);
                /* tell risk assessment not enough locked contracts */
                try {
                    rsk.notice(3003, msg, oCtx);
                } catch (Throwable th) {
                    PlatirSystem.err.write("Risk assessment notice(int, String, OrderContext) throws exception: " + th.getMessage(), th);
                }
            }
        }

        private void timedOnNotice(int code, String message) {
            timedOnNotice(code, message, null);
        }

        private void timedOnNotice(int code, String message, Throwable error) {
            var n = ObjectFactory.newNotice();
            n.setCode(code);
            n.setMessage(message);
            n.setObject(error);
            trCtx.getStrategyContext().timedOnNotice(n);
        }

        private void checkCompleted(int addedVolume) {
            int cur = count.addAndGet(addedVolume);
            var vol = oCtx.getOrder().getVolume();
            if (cur >= vol) {
                /* let garbage collection reclaim the objects */
                oCtx = null;
                trCtx = null;
            }
            if (cur == vol) {
                timedOnNotice(0, "trade completed");
            } else if (cur > vol) {
                int code = 3002;
                var msg = "order(" + oCtx.getOrder().getOrderId() + ") over traded";
                PlatirSystem.err.write(msg);
                /* tell risk assessment there is an order over traded */
                try {
                    saveCodeMessage0(code, msg);
                    rsk.notice(code, msg, oCtx);
                } catch (Throwable th) {
                    PlatirSystem.err.write("Risk assessment notice(int, String, OrderContext) throws exception: " + th.getMessage(), th);
                }
            }
        }

        private void saveCodeMessage0(int code, String message) {
            var r = ObjectFactory.newRiskNotice();
            var profile = trCtx.getStrategyContext().getProfile();
            r.setCode(3002);
            r.setMessage("order(" + oCtx.getOrder().getOrderId() + ") over traded");
            r.setLevel(5);
            r.setUserId(profile.getUserId());
            r.setStrategyId(profile.getStrategyId());
            r.setUpdateTime(PlatirSystem.datetime());
            try {
                trCtx.getQueryClient().queries().insert(r);
            } catch (SQLException e) {
                PlatirSystem.err.write("Can't inert RiskNotice(" + code + ", " + message + "): " + e.getMessage(), e);
            }
        }
    }
}
