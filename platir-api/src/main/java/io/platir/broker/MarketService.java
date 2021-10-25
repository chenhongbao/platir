package io.platir.broker;

public interface MarketService {
    void marketDataRequest(MarketDataListener listener, String... instrumentIds);
}
