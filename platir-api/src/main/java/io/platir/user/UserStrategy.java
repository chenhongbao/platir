package io.platir.user;

import io.platir.Transaction;
import io.platir.broker.Bar;
import io.platir.broker.MarketDataSnapshot;

public interface UserStrategy {
    void onBar(Bar bar);
    
    void onMarketDataSnapshot(MarketDataSnapshot marketDataSnapshot);
    
    void onTransaction(Transaction transaction);
    
    void onLoad(Session session);
    
    void onConfiguredOpen();
    
    void onConfiguredClose();
    
    void onAlarm();
}
