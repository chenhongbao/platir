package io.platir.core.internal;

import io.platir.queries.Utils;
import io.platir.queries.ObjectFactory;
import io.platir.service.Bar;
import io.platir.service.Constants;
import io.platir.service.Notice;
import io.platir.service.Strategy;
import io.platir.service.Tick;
import io.platir.service.Trade;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 *
 * @author Chen Hongbao
 */
class StrategyCallbackQueue implements Runnable {

    private final Strategy strategy;
    private final BlockingQueue<Object> callbackObjects = new LinkedBlockingQueue<>();
    private final Future daemonFuture;

    StrategyCallbackQueue(Strategy strategy) {
        this.strategy = strategy;
        this.daemonFuture = Utils.threads.submit(this);
    }

    void push(Object object) {
        if (!callbackObjects.offer(object)) {
            Utils.err.write("Strategy callback queueing queue is full.");
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
                if (object instanceof Notice) {
                    timedOnNotice((Notice) object);
                } else if (object instanceof Trade) {
                    timedOnTrade((Trade) object);
                } else if (object instanceof Tick) {
                    timedOnTick((Tick) object);
                } else if (object instanceof Bar) {
                    timedOnBar((Bar) object);
                } else {
                    Utils.err.write("Unknown instance: " + object);
                }
            } catch (InterruptedException ex) {
                Utils.err.write("Strategy callback daemon is interrupted.");
            } catch (Throwable th) {
                Utils.err.write("Uncaught error: " + th.getMessage(), th);
            }
        }
    }

    private void timedOperation(boolean needNotice, int timeoutSec, TimedJob job) {
        var future = Utils.threads.submit(() -> {
            var notice = ObjectFactory.newNotice();
            try {
                job.work();
                notice.setCode(Constants.CODE_OK);
                notice.setMessage("good");
            } catch (Throwable th) {
                notice.setCode(Constants.CODE_STRATEGY_EXCEPTION);
                notice.setMessage("callback throws exception: " + th.getMessage());
                notice.setError(th);
            }
            return notice;
        });

        try {
            var taskNotice = future.get(timeoutSec, TimeUnit.SECONDS);
            if (!taskNotice.isGood()) {
                Utils.err.write(taskNotice.getMessage());
                if (needNotice) {
                    /* Tell strategy its callback fails. */
                    timedOnNotice(taskNotice);
                }
            }
        } catch (InterruptedException | ExecutionException exception) {
            Utils.err.write("Timed operation is interrupted: " + exception.getMessage(), exception);
        } catch (TimeoutException exception) {
            var notice = ObjectFactory.newNotice();
            notice.setCode(Constants.CODE_STRATEGY_TIMEOUT);
            notice.setMessage("callback operation is timeout");
            notice.setError(exception);
            /* Tell strategy its callback timeout. */
            timedOnNotice(notice);
        } finally {
            /* The task has to be aborted. */
            if (!future.isDone()) {
                future.cancel(true);
            }
        }
    }

    private void timedOnBar(Bar bar) {
        timedOperation(true, 1, () -> {
            strategy.onBar(bar);
        });
    }

    private void timedOnTick(Tick tick) {
        timedOperation(true, 1, () -> {
            strategy.onTick(tick);
        });
    }

    private void timedOnTrade(Trade trade) {
        timedOperation(true, 1, () -> {
            strategy.onTrade(trade);
        });
    }

    private void timedOnNotice(Notice notice) {
        timedOperation(false, 1, () -> {
            strategy.onNotice(notice);
        });
    }

    void timedOnStart(String[] args, PlatirClientImpl cli) {
        timedOperation(true, 5, () -> {
            strategy.onStart(args, cli);
        });
    }

    void timedOnStop(int reason) {
        timedOperation(true, 5, () -> {
            strategy.onStop(reason);
        });
    }

    void timedOnDestroy() {
        timedOperation(true, 5, () -> {
            strategy.onDestroy();
        });
    }

    @FunctionalInterface
    private interface TimedJob {

        void work();
    }
}
