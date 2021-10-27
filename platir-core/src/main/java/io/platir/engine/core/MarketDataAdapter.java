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
import java.util.concurrent.atomic.AtomicInteger;

class MarketDataAdapter implements MarketDataListener {

    private final MarketDataService marketDataService;
    private final UserStrategyLookup userStrategyManager;
    private final Boolean isParallel;
    private final Map<String, StrategyMarketDataAdapter> strategies = new ConcurrentHashMap<>();
    private final AtomicInteger currentTradingDayHashCode = new AtomicInteger(0);

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
        tryUpdateTradingDay(marketDataSnapshot.getTradingDay());
    }

    @Override
    public void onBar(Bar bar) {
        var adapter = strategies.get(bar.getInstrumentId());
        if (adapter != null) {
            adapter.broadcast(bar);
        }
        tryUpdateTradingDay(bar.getTradingDay());
    }

    void marketDataRequest(Strategy strategy, String instrumentId) throws MarketDataRequestException {
        if (!strategies.containsKey(instrumentId)) {
            var code = marketDataService.marketDataRequest(instrumentId, this);
            if (code != 0) {
                throw new MarketDataRequestException("Market data request returns " + code + ".");
            }
        }
        strategies.computeIfAbsent(instrumentId, key -> new StrategyMarketDataAdapter(userStrategyManager, isParallel)).add(strategy);
    }

    void registerStrategy(Strategy newStrategy) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    void unblockStrategy(Strategy strategy) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    void blockStrategy(Strategy strategy) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    private void tryUpdateTradingDay(String tradingDay) {
        var newHashCode = tradingDay.hashCode();
        if (currentTradingDayHashCode.get() != newHashCode) {
            InfoCenter.setTradingDay(tradingDay);
            currentTradingDayHashCode.set(newHashCode);
        }
    }
}
