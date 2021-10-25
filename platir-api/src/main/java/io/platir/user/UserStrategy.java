package io.platir.user;

import io.platir.Transaction;
import io.platir.broker.Bar;
import io.platir.broker.MarketDataSnapshot;

public interface UserStrategy {
    void onBar(Bar bar);
    
    void onMarketDataSnapshot(MarketDataSnapshot marketDataSnapshot);
    
    void onTransaction(Transaction transaction);
    
    void onStart(Session session);
    
    void onStop();
}
