package io.platir.core.internal;

import io.platir.queries.Utils;
import io.platir.service.Bar;
import io.platir.service.Factory;
import io.platir.service.Strategy;
import io.platir.service.Tick;
import io.platir.service.Trade;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import io.platir.service.TradeUpdate;

/**
 *
 * @author Chen Hongbao
 */
class StrategyCallbackQueue implements Runnable {

    private final Strategy strategy;
    private final Factory factory;
    private final BlockingQueue<Object> callbackObjects = new LinkedBlockingQueue<>();
    private final Future daemonFuture;

    StrategyCallbackQueue(Strategy strategy, Factory factory) {
        this.strategy = strategy;
        this.factory = factory;
        this.daemonFuture = Utils.threads().submit(this);
    }

    void push(Object object) {
        if (!callbackObjects.offer(object)) {
            Utils.err().write("Strategy callback queueing queue is full.");
        }
    }

    void shutdown() {
        daemonFuture.cancel(true);
    }

    @Override
    public void run() {
        while (!Thread.currentThread().isInterrupted() || !callbackObjects.isEmpty()) {
            try {
                var object = callbackObjects.poll(24, TimeUnit.HOURS);
                if (object instanceof TradeUpdate) {
                    timedOnNotice((TradeUpdate) object);
                } else if (object instanceof Trade) {
                    timedOnTrade((Trade) object);
                } else if (object instanceof Tick) {
                    timedOnTick((Tick) object);
                } else if (object instanceof Bar) {
                    timedOnBar((Bar) object);
                } else {
                    Utils.err().write("Unknown instance: " + object);
                }
            } catch (InterruptedException ex) {
                Utils.err().write("Strategy callback daemon is interrupted.");
            } catch (Throwable th) {
                Utils.err().write("Uncaught error: " + th.getMessage(), th);
            }
        }
    }

    private void timedOperation(int timeoutSec, TimedJob job) {
        var future = Utils.threads().submit(() -> {
            try {
                job.work();
            } catch (Throwable throwable) {
                Utils.err().write("Strategy callback throws exception: " + throwable.getMessage(), throwable);
            }
        });

        try {
            future.get(timeoutSec, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException exception) {
            Utils.err().write("Timed operation is interrupted: " + exception.getMessage(), exception);
        } catch (TimeoutException exception) {
            Utils.err().write("Strategy callback timeout: " + exception.getMessage(), exception);
        } finally {
            /* The task has to be aborted. */
            if (!future.isDone()) {
                future.cancel(true);
            }
        }
    }

    private void timedOnBar(Bar bar) {
        timedOperation(1, () -> {
            strategy.onBar(bar);
        });
    }

    private void timedOnTick(Tick tick) {
        timedOperation(1, () -> {
            strategy.onTick(tick);
        });
    }

    private void timedOnTrade(Trade trade) {
        timedOperation(1, () -> {
            strategy.onTrade(trade);
        });
    }

    private void timedOnNotice(TradeUpdate updateNotice) {
        timedOperation(1, () -> {
            strategy.onTradeUpdate(updateNotice);
        });
    }

    void timedOnStart(String[] args, PlatirClientImpl cli) {
        timedOperation(5, () -> {
            strategy.onStart(args, cli);
        });
    }

    void timedOnStop(int reason) {
        timedOperation(5, () -> {
            strategy.onStop(reason);
        });
    }

    void timedOnDestroy() {
        timedOperation(5, () -> {
            strategy.onDestroy();
        });
    }

    @FunctionalInterface
    private interface TimedJob {

        void work();
    }
}
