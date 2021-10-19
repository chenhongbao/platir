package io.platir.core.internal;

import io.platir.core.internal.objects.ObjectFactory;
import io.platir.service.Notice;
import io.platir.service.Trade;
import io.platir.service.api.TradeListener;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import io.platir.service.api.RiskManager;

/**
 * Error code explaination:
 * <ul>
 * <li>3001: Trade response timeout.</li>
 * <li>3002: Trade more than expected.</li>
 * <li>3003: Locked contracts are less than traded contracts.</li>
 * </ul>
 */
class TradeListenerContexts implements TradeListener {

    private final Map<String, OrderExecutionContext> ctxs = new ConcurrentHashMap<>();
    private final RiskManager rsk;
    private final TradeCallbackQueue tradeQueue;
    private final NoticeCallbackQueue noticeQueue;

    TradeListenerContexts(RiskManager risk) {
        rsk = risk;
        tradeQueue = new TradeCallbackQueue();
        noticeQueue = new NoticeCallbackQueue();
        Utils.threads.submit(tradeQueue);
        Utils.threads.submit(noticeQueue);
    }

    int countStrategyRunning(StrategyContextImpl strategy) {
        return ctxs.values().stream().mapToInt(ctx -> ctx.transactionContext.getStrategyContext() == strategy ? 1 : 0).sum();
    }

    void clearContexts() {
        ctxs.clear();
    }

    void register(OrderContextImpl orderCtx, TransactionContextImpl ctx) throws DuplicatedOrderException {
        var orderId = orderCtx.getOrder().getOrderId();
        if (ctxs.containsKey(orderId)) {
            throw new DuplicatedOrderException("Order(" + orderId + ") duplicated.");
        }
        ctxs.put(orderId, new OrderExecutionContext(orderCtx, ctx, rsk));
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
            tradeQueue.push(trade, ctx);
        } else {
            Utils.err.write("Order execution context not found for order(" + trade.getOrderId() + ").");
        }
    }

    @Override
    public void onNotice(String orderId, int code, String message) {
        var ctx = ctxs.get(orderId);
        if (ctx != null) {
            var n = ObjectFactory.newNotice();
            n.setCode(code);
            n.setMessage(message);
            noticeQueue.push(n, ctx);
        } else {
            Utils.err.write("Order execution context not found for order(" + orderId + ").");
        }
    }

}