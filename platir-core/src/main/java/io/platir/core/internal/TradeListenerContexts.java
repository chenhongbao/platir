package io.platir.core.internal;

import io.platir.queries.Utils;
import io.platir.service.DataQueryException;
import io.platir.service.Factory;
import io.platir.service.Notice;
import io.platir.service.Queries;
import io.platir.service.Trade;
import io.platir.service.api.TradeListener;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import io.platir.service.api.RiskManager;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Error code explaination:
 * <ul>
 * <li>3001: Trade response timeout.</li>
 * <li>3002: Trade more than expected.</li>
 * <li>3003: Locked contracts are less than traded contracts.</li>
 * </ul>
 */
class TradeListenerContexts implements TradeListener {

    private final Map<String, OrderExecutionContext> executionContexts = new ConcurrentHashMap<>();
    private final RiskManager riskManager;
    private final TradeCallbackQueue tradeQueue;
    private final NoticeCallbackQueue noticeQueue;
    private final Factory factory;
    private final AtomicInteger tradingDayHashCode = new AtomicInteger(0);

    TradeListenerContexts(RiskManager riskManager, Factory factory) {
        this.riskManager = riskManager;
        this.factory = factory;
        this.tradeQueue = new TradeCallbackQueue();
        this.noticeQueue = new NoticeCallbackQueue();
        Utils.threads().submit(tradeQueue);
        Utils.threads().submit(noticeQueue);
    }

    int countStrategyRunning(StrategyContextImpl strategy) {
        return executionContexts.values().stream().mapToInt(executionContext -> executionContext.getTransactionContext().getStrategyContext() == strategy ? 1 : 0).sum();
    }

    void clearContexts() {
        executionContexts.clear();
    }

    void register(OrderContextImpl orderCtx, TransactionContextImpl ctx) throws DuplicatedOrderException {
        var orderId = orderCtx.getOrder().getOrderId();
        if (executionContexts.containsKey(orderId)) {
            throw new DuplicatedOrderException("Order(" + orderId + ") duplicated.");
        }
        executionContexts.put(orderId, new OrderExecutionContext(orderCtx, ctx, riskManager));
    }

    Notice waitResponse(String orderId) {
        var ctx = executionContexts.get(orderId);
        if (ctx != null) {
            return ctx.waitResponse();
        } else {
            throw new RuntimeException("Order execution context not found for order(" + orderId + ").");
        }
    }

    @Override
    public void onTrade(Trade trade) {
        var ctx = executionContexts.get(trade.getOrderId());
        if (ctx != null) {
            tradeQueue.push(trade, ctx);
            /* Update trading day if possible. */
            updateTradingDay(trade.getTradingDay(), ctx.getTransactionContext().getQueryClient().queries());
        } else {
            Utils.err().write("Order execution context not found for order(" + trade.getOrderId() + ").");
        }
    }

    @Override
    public void onNotice(String orderId, int code, String message) {
        var context = executionContexts.get(orderId);
        if (context != null) {
            var notice = factory.newNotice();
            notice.setCode(code);
            notice.setMessage(message);
            notice.setContext(context.getTransactionContext());
            noticeQueue.push(notice, context);
        } else {
            Utils.err().write("Order execution context not found for order(" + orderId + ").");
        }
    }

    private void updateTradingDay(String newTradingDay, Queries queries) {
        if (newTradingDay != null && newTradingDay.hashCode() != tradingDayHashCode.get()) {
            var day = queries.getFactory().newTradingDay();
            day.setDay(newTradingDay);
            day.setUpdateTime(Utils.datetime());
            try {
                var oldTradingDay = queries.selectTradingDay();
                if (oldTradingDay == null || oldTradingDay.getDay() == null) {
                    queries.insert(day);
                } else {
                    queries.update(day);
                }
                tradingDayHashCode.set(newTradingDay.hashCode());
            } catch (DataQueryException exception) {
                Utils.err().write("Fail updating trading day: " + exception.getMessage(), exception);
            }
        }
    }
}
