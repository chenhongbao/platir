package io.platir.engine.core;

import io.platir.Strategy;
import io.platir.broker.Bar;
import io.platir.broker.MarketDataListener;
import io.platir.broker.MarketDataResponse;
import io.platir.broker.MarketDataService;
import io.platir.broker.MarketDataSnapshot;
import io.platir.user.MarketDataRequestException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

class MarketDataAdapter implements MarketDataListener {

    private final MarketDataService marketDataService;
    private final UserStrategyLookup userStrategyManager;
    private final Map<String, StrategyMarketDataAdapter> strategies = new ConcurrentHashMap<>();

    MarketDataAdapter(MarketDataService marketDataService, UserStrategyLookup userStrategyManager) {
        this.marketDataService = marketDataService;
        this.userStrategyManager = userStrategyManager;
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
        if (!strategies.containsKey(instrumentId)) {
            var code = marketDataService.marketDataRequest(instrumentId, this);
            if (code != 0) {
                throw new MarketDataRequestException("Market data request returns " + code + ".");
            }
        }
        strategies.computeIfAbsent(instrumentId, key -> new StrategyMarketDataAdapter(userStrategyManager)).add(strategy);
    }
}
