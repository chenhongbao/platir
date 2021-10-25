package io.platir.user;

import io.platir.Account;
import io.platir.Transaction;
import java.util.Map;
import java.util.logging.Logger;

public interface Session {
    Transaction buyOpen(String instrumentId, String exchangeId, Double price, Integer quantity) throws NewOrderException;
    
    Transaction sellOpen(String instrumentId, String exchangeId, Double price, Integer quantity) throws NewOrderException;
    
    Transaction buyClose(String instrumentId, String exchangeId, Double price, Integer quantity) throws NewOrderException;
    
    Transaction sellClose(String instrumentId, String exchangeId, Double price, Integer quantity) throws NewOrderException;
    
    String marketDataRequest(String instrumentId) throws MarketDataRequestException;
    
    Account getAccount();
    
    Logger getLogger();
    
    Map<String, String> getParameters();
}
