package io.platir.user;

import io.platir.Account;
import io.platir.Transaction;
import java.util.Map;
import java.util.logging.Logger;

public interface Session {
    Transaction buyOpen(String instrumentId, String exchangeId, Double price, Integer quantity);
    
    Transaction sellOpen(String instrumentId, String exchangeId, Double price, Integer quantity);
    
    Transaction buyClose(String instrumentId, String exchangeId, Double price, Integer quantity);
    
    Transaction sellClose(String instrumentId, String exchangeId, Double price, Integer quantity);
    
    Account getAccount();
    
    Logger getLogger();
    
    Map<String, String> getParameters();
}
