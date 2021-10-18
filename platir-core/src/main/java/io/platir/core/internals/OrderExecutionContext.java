package io.platir.core.internals;

import io.platir.core.internals.persistence.object.ObjectFactory;
import io.platir.service.Notice;
import io.platir.service.RiskNotice;
import io.platir.service.Trade;
import io.platir.service.api.DataQueryException;
import io.platir.service.api.RiskAssess;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 *
 * @author Chen Hongbao
 */
class OrderExecutionContext {

    private Notice notice;
    private OrderContextImpl oCtx;
    TransactionContextImpl trCtx;
    private final AtomicInteger count = new AtomicInteger(0);
    private final RiskAssess rsk;

    /* first notice waiting facilities */
    private final Lock l = new ReentrantLock();
    private final Condition cond = l.newCondition();
    private final int timeoutSec = 5;

    OrderExecutionContext(OrderContextImpl orderCtx, TransactionContextImpl ctx, RiskAssess risk) {
        oCtx = orderCtx;
        trCtx = ctx;
        rsk = risk;
    }

    void processTrade(Trade trade) {
        io.platir.core.internals.StrategyContextImpl stg = trCtx.getStrategyContext();
        try {
            /* save trade to data source. */
            stg.getPlatirClientImpl().queries().insert(trade);
        } catch (DataQueryException e) {
            /* worker thread sees this exception, just log it */
            Utils.err.write("Can't insert trade(" + trade.getTradeId() + ") for transaction(" + trCtx.getTransaction() + ") and strategy(" + stg.getProfile().getStrategyId() + ") into data source: " + e.getMessage(), e);
        }
        /* add trade to order context. */
        oCtx.addTrade(trade);
        /* update contracts' states */
        updateContracts(trade);
        stg.processTrade(trade);
        checkCompleted(trade.getVolume());
        /* risk assess */
        afterRisk(trade);
    }

    void processNotice(int code, String message) {
        signalResponse(code, message);
        pushNotice(code, message);
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
                TransactionFacilities.saveRiskNotice(r.getCode(), r.getMessage(), RiskNotice.WARNING, trCtx);
            }
        } catch (Throwable th) {
            Utils.err.write("Risk assess after() throws exception: " + th.getMessage(), th);
            TransactionFacilities.saveRiskNotice(1005, "after(Trade) throws exception", RiskNotice.ERROR, trCtx);
        }
    }

    private void updateContracts(Trade trade) {
        io.platir.core.internals.StrategyContextImpl stg = trCtx.getStrategyContext();
        int updateCount = 0;
        java.util.Iterator<io.platir.service.Contract> it = oCtx.lockedContracts().iterator();
        while (++updateCount <= trade.getVolume() && it.hasNext()) {
            io.platir.service.Contract c = it.next();
            java.lang.String prevState = c.getState();
            if (c.getState().compareToIgnoreCase("opening") == 0) {
                /* Update open price because the real traded price may be different. */
                c.setState("open");
                c.setPrice(trade.getPrice());
                c.setOpenTime(Utils.datetime());
                c.setOpenTradingDay(stg.getPlatirClientImpl().getTradingDay());
            } else if (c.getState().compareToIgnoreCase("closing") == 0) {
                /* don't forget the close price here */
                c.setState("closed");
                c.setClosePrice(trade.getPrice());
            } else {
                Utils.err.write("Incorrect contract state(" + c.getState() + "/" + c.getContractId() + ") before completing trade.");
                continue;
            }
            try {
                stg.getPlatirClientImpl().queries().update(c);
            } catch (DataQueryException e) {
                Utils.err.write("Fail updating user(" + c.getUserId() + ") contract(" + c.getContractId() + ") state(" + c.getState() + ").", e);
                /* roll back state */
                c.setState(prevState);
                continue;
            }
            it.remove();
        }
        if (updateCount <= trade.getVolume()) {
            java.lang.String msg = "Insufficent(" + updateCount + "<" + trade.getVolume() + ") locked contracts.";
            Utils.err.write(msg);
        }
    }

    private void pushNotice(int code, String message) {
        pushNotice(code, message, null);
    }

    private void pushNotice(int code, String message, Throwable error) {
        io.platir.service.Notice n = ObjectFactory.newNotice();
        n.setCode(code);
        n.setMessage(message);
        n.setObject(error);
        trCtx.getStrategyContext().processNotice(n);
    }

    private void checkCompleted(int addedVolume) {
        int cur = count.addAndGet(addedVolume);
        java.lang.Integer vol = oCtx.getOrder().getVolume();
        if (cur >= vol) {
            /* let garbage collection reclaim the objects */
            oCtx = null;
            trCtx = null;
        }
        if (cur == vol) {
            pushNotice(0, "trade completed");
        } else if (cur > vol) {
            int code = 3002;
            java.lang.String msg = "order(" + oCtx.getOrder().getOrderId() + ") over traded";
            Utils.err.write(msg);
        }
    }

}
