package io.platir.core.internals;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;

import io.platir.core.PlatirSystem;
import io.platir.service.Tick;
import io.platir.service.api.MarketAdaptor;
import io.platir.service.api.MarketListener;

class MarketRouter implements MarketListener {

    private final Map<String, Set<StrategyContextImpl>> subs = new ConcurrentHashMap<>();
    private final Map<String, Tick> ticks = new ConcurrentHashMap<>();
    private final TransactionQueue trQueue;
    private final MarketAdaptor adaptor;

    MarketRouter(MarketAdaptor market, TransactionQueue queue) {
        adaptor = market;
        trQueue = queue;
        adaptor.setListener(this);
    }

    Set<Tick> getLastTicks() {
        return new HashSet<>(ticks.values());
    }

    void updateSubscription(StrategyContextImpl strategy) {
        if (strategy.getProfile().getInstrumentIds().length == 0) {
            removeSubscription(strategy);
        } else {
            var ns = new HashSet<>(Arrays.asList(strategy.getProfile().getInstrumentIds()));
            subs.entrySet().stream().filter(entry -> (!ns.contains(entry.getKey()))).forEachOrdered(entry -> {
                /* instrument that is not subscribed, remove it */
                entry.getValue().remove(strategy);
            });
            ns.forEach(i -> {
                subscribe(i, strategy);
            });
        }
    }

    void refreshAllSubscriptions() {
        subs.keySet().forEach(key -> {
            var tick = ticks.get(key);
            if (tick != null) {
                var datetime = PlatirSystem.datetime(tick.getUpdateTime());
                /* if tick doesn't arrive for over 30 days, the instrument has expired. */
                if (Duration.between(datetime, LocalDateTime.now()).toDays() < 30) {
                    adaptor.add(key);
                }
            }
        });
    }

    void subscribe(StrategyContextImpl strategy) {
        for (var i : strategy.getProfile().getInstrumentIds()) {
            subscribe(i, strategy);
        }
    }

    void removeSubscription(StrategyContextImpl strategy) {
        for (var i : strategy.getProfile().getInstrumentIds()) {
            var p = subs.get(i);
            if (p == null) {
                continue;
            }
            p.remove(strategy);
            /* if no strategy subscribes the instrument, remove it from market */
            if (p.isEmpty()) {
                subs.remove(i);
            }
        }
    }

    private void subscribe(String instrumentId, StrategyContextImpl strategy) {
        var p = subs.computeIfAbsent(instrumentId, key -> {
            adaptor.add(key);
            return new ConcurrentSkipListSet<>();
        });
        if (!p.contains(strategy)) {
            p.add(strategy);
        }
    }

    @Override
    public void onTick(Tick tick) {
        var x = subs.get(tick.getInstrumentId());
        x.parallelStream().forEach(ctx -> {
            ctx.processTick(tick);
        });
        /* signal transaction queue to work on pending transactions. */
        tryAwake(tick);
        /* save ticks for settlement */
        ticks.put(tick.getInstrumentId(), tick);
    }

    private void tryAwake(Tick tick) {
        var ut = tick.getUpdateTime();
        if (ut.length() != 17) {
            PlatirSystem.err.write("Malformed update time " + ut + ".");
            return;
        }
        var sec = tick.getUpdateTime().substring(15, 16);
        if (sec.equals("00") || sec.equals("59")) {
            /*don't awake transaction at the edge of a minute, it may be the end of a session */
            return;
        }
        trQueue.awake(tick);
    }
}
