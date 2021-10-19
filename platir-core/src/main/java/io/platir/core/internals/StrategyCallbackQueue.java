package io.platir.core.internals;

import io.platir.core.internals.persistence.object.ObjectFactory;
import io.platir.service.Bar;
import io.platir.service.Notice;
import io.platir.service.Strategy;
import io.platir.service.StrategyProfile;
import io.platir.service.Tick;
import io.platir.service.Trade;
import io.platir.service.api.DataQueryException;
import io.platir.service.api.RiskAssess;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Chen Hongbao
 */
class StrategyCallbackQueue implements Runnable {

    private final RiskAssess rsk;
    private final Strategy stg;
    private final StrategyProfile prof;
    private final PlatirClientImpl cli;
    private final BlockingQueue<Object> q = new LinkedBlockingQueue<>();
    private final Future daemonFut;

    StrategyCallbackQueue(PlatirClientImpl client, StrategyProfile profile, RiskAssess risk, Strategy strategy) {
        cli = client;
        rsk = risk;
        stg = strategy;
        prof = profile;
        daemonFut = Utils.threads.submit(this);
    }

    void push(Object object) {
        if (!q.offer(object)) {
            Utils.err.write("Strategy callback queueing queue is full.");
        }
    }

    void shutdown() {
        daemonFut.cancel(true);
    }

    @Override
    public void run() {
        while (!Thread.currentThread().isInterrupted() || !q.isEmpty()) {
            try {
                var object = q.poll(24, TimeUnit.HOURS);
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
                Logger.getLogger(StrategyCallbackQueue.class.getName()).log(Level.SEVERE, null, ex);
            } catch (Throwable th) {
                Utils.err.write("Uncaught error: " + th.getMessage(), th);
            }
        }
    }

    private void timedOperation(boolean needNotice, int timeoutSec, TimedJob job) {
        var timedFut = Utils.threads.submit(() -> {
            var r = ObjectFactory.newNotice();
            try {
                job.work();
                r.setCode(0);
                r.setMessage("good");
            } catch (Throwable th) {
                r.setCode(4001);
                r.setMessage("Callback throws exception: " + th.getMessage());
                r.setError(th);
            }
            return r;
        });

        try {
            var r = timedFut.get(timeoutSec, TimeUnit.SECONDS);
            if (!r.isGood()) {
                Utils.err.write(r.getMessage());
                if (needNotice) {
                    /* tell strategy its callback fails */
                    timedOnNotice(r);
                }
            }
        } catch (InterruptedException | ExecutionException e) {
            Utils.err.write("Timed operation is interrupted: " + e.getMessage(), e);
        } catch (TimeoutException e) {
            var r = ObjectFactory.newNotice();
            r.setCode(4002);
            r.setMessage("Callback operation is timeout.");
            r.setError(e);
            /* tell strategy its callback timeout */
            timedOnNotice(r);
        } finally {
            /* the task has to be aborted */
            if (!timedFut.isDone()) {
                timedFut.cancel(true);
            }
        }
    }

    private void timedOnBar(Bar bar) {
        timedOperation(true, 1, () -> {
            stg.onBar(bar);
        });
    }

    private void timedOnTick(Tick tick) {
        timedOperation(true, 1, () -> {
            stg.onTick(tick);
        });
    }

    private void timedOnTrade(Trade trade) {
        timedOperation(true, 1, () -> {
            stg.onTrade(trade);
        });
    }

    private void timedOnNotice(Notice notice) {
        timedOperation(false, 1, () -> {
            stg.onNotice(notice);
        });
    }

    void timedOnStart(String[] args, PlatirClientImpl cli) {
        timedOperation(true, 5, () -> {
            stg.onStart(args, cli);
        });
    }

    void timedOnStop(int reason) {
        timedOperation(true, 5, () -> {
            stg.onStop(reason);
        });
    }

    void timedOnDestroy() {
        timedOperation(true, 5, () -> {
            stg.onDestroy();
        });
    }

    @FunctionalInterface
    private interface TimedJob {

        void work();
    }
}
