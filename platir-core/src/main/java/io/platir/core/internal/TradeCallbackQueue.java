package io.platir.core.internal;

import io.platir.queries.Utils;
import io.platir.service.Trade;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 *
 * @author Chen Hongbao
 */
class TradeCallbackQueue implements Runnable {

    private final BlockingQueue<TradeCallbackBundle> bundles = new LinkedBlockingQueue<>();

    void push(Trade trade, OrderExecutionContext context) {
        if (!bundles.offer(new TradeCallbackBundle(trade, context))) {
            Utils.err().write("Trade callback queue is full.");
        }
    }

    @Override
    public void run() {
        while (!Thread.currentThread().isInterrupted() || !bundles.isEmpty()) {
            try {
                var bundle = bundles.poll(24, TimeUnit.HOURS);
                bundle.executionContext.processTrade(bundle.trade);
            } catch (InterruptedException ex) {
                Utils.err().write("Trade callback queue daemon is interrupted.", ex);
            } catch (Throwable th) {
                Utils.err().write("Uncaught error: " + th.getMessage(), th);
            }
        }
    }

}
