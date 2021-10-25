package io.platir.broker;

import java.util.Map;

public interface MarketDataService {

    void marketDataRequest(MarketDataListener listener, String... instrumentIds);

    void initialize(Map<String, String> parameters);

    Map<String, String> getParameterHints();
    
    String getVersion();
}
