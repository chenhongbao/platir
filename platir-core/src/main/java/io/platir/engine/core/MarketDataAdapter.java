package io.platir.engine.core;

import io.platir.commons.StrategyCore;
import io.platir.Strategy;
import io.platir.broker.Bar;
import io.platir.broker.MarketDataListener;
import io.platir.broker.MarketDataResponse;
import io.platir.broker.MarketDataService;
import io.platir.broker.MarketDataSnapshot;
import io.platir.user.MarketDataRequestException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

class MarketDataAdapter implements MarketDataListener {

    private final MarketDataService marketDataService;
    private final UserStrategyLookup userStrategyManager;
    private final Boolean isParallel;
    private final Map<String, StrategyMarketDataAdapter> strategies = new ConcurrentHashMap<>();

    MarketDataAdapter(MarketDataService marketDataService, UserStrategyLookup userStrategyManager, Boolean parallel) {
        this.marketDataService = marketDataService;
        this.userStrategyManager = userStrategyManager;
        this.isParallel = parallel;
    }

    @Override
    public void onMarketDataResponse(MarketDataResponse marketDataResponse) {
        // Ignore.
    }

    @Override
    public void onMarketDataSnapshot(MarketDataSnapshot marketDataSnapshot) {
        var adapter = strategies.get(marketDataSnapshot.getInstrumentId());
        if (adapter != null) {
            adapter.broadcast(marketDataSnapshot);
        }
    }

    @Override
    public void onBar(Bar bar) {
        var adapter = strategies.get(bar.getInstrumentId());
        if (adapter != null) {
            adapter.broadcast(bar);
        }
    }

    void marketDataRequest(Strategy strategy, String instrumentId) throws MarketDataRequestException {
        if (!strategy.getState().equals(Strategy.NORMAL)) {
            throw new MarketDataRequestException("Strategy(" + strategy.getStrategyId() + ") is " + strategy.getState() + ".");
        }
        if (!strategies.containsKey(instrumentId)) {
            var code = marketDataService.marketDataRequest(instrumentId, this);
            if (code != 0) {
                throw new MarketDataRequestException("Market data request returns " + code + ".");
            }
        }
        strategies.computeIfAbsent(instrumentId, key -> new StrategyMarketDataAdapter(userStrategyManager, isParallel)).add((StrategyCore) strategy);
        tryRemoveDeadSubscription();
    }

    private void tryRemoveDeadSubscription() {
        final var now = LocalDateTime.now();
        strategies.values().removeIf(adapter -> Duration.between(adapter.getTimestamp(), now).toDays() > 30);
    }
}
