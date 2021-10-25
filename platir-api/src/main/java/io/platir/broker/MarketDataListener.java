package io.platir.broker;

public interface MarketDataListener {
    
    void onMarketDataResponse(MarketDataResponse marketDataResponse);
    
    void onMarketDataSnapshot(MarketDataSnapshot marketDataSnapshot);
    
    void onBar(Bar bar);
}
