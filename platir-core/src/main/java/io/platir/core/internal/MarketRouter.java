package io.platir.core.internal;

import io.platir.queries.Utils;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;

import io.platir.service.Tick;
import io.platir.service.api.MarketAdaptor;
import io.platir.service.api.MarketListener;

class MarketRouter implements MarketListener {

    private final Map<String, Set<StrategyContextImpl>> subscribedStrategies = new ConcurrentHashMap<>();
    private final Map<String, Tick> ticks = new ConcurrentHashMap<>();
    private final TransactionQueue transactionQueue;
    private final MarketAdaptor marketAdaptor;

    MarketRouter(MarketAdaptor marketAdaptor, TransactionQueue transactionQueue) {
        this.marketAdaptor = marketAdaptor;
        this.transactionQueue = transactionQueue;
        this.marketAdaptor.setListener(this);
    }

    Set<Tick> getLastTicks() {
        return new HashSet<>(ticks.values());
    }

    void updateSubscription(StrategyContextImpl strategy) {
        if (strategy.getProfile().getInstrumentIds().length == 0) {
            removeSubscription(strategy);
        } else {
            var instrumentsToUpdate = new HashSet<>(Arrays.asList(strategy.getProfile().getInstrumentIds()));
            subscribedStrategies.entrySet().stream().filter(entry -> !instrumentsToUpdate.contains(entry.getKey())).forEachOrdered(entry -> {
                /* instrument that is not subscribed, remove it */
                entry.getValue().remove(strategy);
            });
            instrumentsToUpdate.forEach(instrumentId -> {
                subscribe(instrumentId, strategy);
            });
        }
    }

    void refreshAllSubscriptions() {
        subscribedStrategies.keySet().forEach(instrumentId -> {
            var tick = ticks.get(instrumentId);
            if (tick != null) {
                var datetime = Utils.datetime(tick.getUpdateTime());
                /* if tick doesn't arrive for over 30 days, the instrument has expired. */
                if (Duration.between(datetime, LocalDateTime.now()).toDays() < 30) {
                    marketAdaptor.add(instrumentId);
                }
            }
        });
    }

    void subscribe(StrategyContextImpl strategyContext) {
        for (var instrumentId : strategyContext.getProfile().getInstrumentIds()) {
            subscribe(instrumentId, strategyContext);
        }
    }

    void removeSubscription(StrategyContextImpl strategyContext) {
        for (var instrumentId : strategyContext.getProfile().getInstrumentIds()) {
            var strategies = subscribedStrategies.get(instrumentId);
            if (strategies == null) {
                continue;
            }
            strategies.remove(strategyContext);
            /* if no strategy subscribes the instrument, remove it from market */
            if (strategies.isEmpty()) {
                subscribedStrategies.remove(instrumentId);
            }
        }
    }

    private void subscribe(String instrumentId, StrategyContextImpl strategyContext) {
        var strategies = subscribedStrategies.computeIfAbsent(instrumentId, key -> {
            marketAdaptor.add(key);
            return new ConcurrentSkipListSet<>();
        });
        if (!strategies.contains(strategyContext)) {
            strategies.add(strategyContext);
        }
    }

    @Override
    public void onTick(Tick tick) {
        var strategies = subscribedStrategies.get(tick.getInstrumentId());
        strategies.parallelStream().forEach(context -> {
            context.processTick(tick);
        });
        /* signal transaction queue to work on pending transactions. */
        tryAwake(tick);
        /* save ticks for settlement */
        ticks.put(tick.getInstrumentId(), tick);
    }

    private void tryAwake(Tick tick) {
        var updateTime = tick.getUpdateTime();
        if (updateTime.length() != 17) {
            Utils.err.write("Malformed update time " + updateTime + ".");
            return;
        }
        var updateSeconds = tick.getUpdateTime().substring(15, 16);
        if (updateSeconds.equals("00") || updateSeconds.equals("59")) {
            /*don't awake transaction at the edge of a minute, it may be the end of a session */
            return;
        }
        transactionQueue.awake(tick);
    }
}
