package io.platir.service.api;

public interface MarketAdaptor {

    void start() throws AdaptorStartupException;;
    
    void setListener(MarketListener listener);

    void add(String instrumentId);
}
