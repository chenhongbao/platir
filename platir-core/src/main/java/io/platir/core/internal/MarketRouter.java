package io.platir.core.internal;

import io.platir.queries.Utils;
import io.platir.service.DataQueryException;
import io.platir.service.Queries;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;

import io.platir.service.Tick;
import io.platir.service.api.MarketListener;
import java.util.concurrent.atomic.AtomicInteger;
import io.platir.service.api.MarketAdapter;

class MarketRouter implements MarketListener {

    private final Map<String, Set<StrategyContextImpl>> subscribedStrategies = new ConcurrentHashMap<>();
    private final Map<String, Tick> ticks = new ConcurrentHashMap<>();
    private final TransactionQueue transactionQueue;
    private final MarketAdapter marketAdaptor;
    private final AtomicInteger tradingDayHashCode = new AtomicInteger(0);
    private final Queries queries;

    MarketRouter(MarketAdapter marketAdaptor, TransactionQueue transactionQueue, Queries queries) {
        this.marketAdaptor = marketAdaptor;
        this.transactionQueue = transactionQueue;
        this.marketAdaptor.setListener(this);
        this.queries = queries;
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
                    marketAdaptor.subscribe(instrumentId);
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
            marketAdaptor.subscribe(key);
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
        /* Signal transaction queue to work on pending transactions. */
        tryAwake(tick);
        /* Save ticks for settlement. */
        ticks.put(tick.getInstrumentId(), tick);
        /* Save trading day if it updated. */
        updateTradingDay(tick.getTradingDay());
    }

    private void tryAwake(Tick tick) {
        var updateTime = tick.getUpdateTime();
        if (updateTime.length() != 17) {
            Utils.err().write("Malformed update time " + updateTime + ".");
            return;
        }
        var updateSeconds = tick.getUpdateTime().substring(15, 16);
        if (updateSeconds.equals("00") || updateSeconds.equals("59")) {
            /*don't awake transaction at the edge of a minute, it may be the end of a session */
            return;
        }
        transactionQueue.awake(tick);
    }

    private void updateTradingDay(String newTradingDay) {
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
