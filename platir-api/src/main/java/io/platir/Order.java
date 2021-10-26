package io.platir;

import java.util.Collection;

public interface Order {

    final static String ALL_TRADED = "ALL_TRADED";

    final static String QUEUEING = "QUEUEING";

    final static String CANCELED = "CANCELED";

    final static String RJECTED = "REJECTED";
    
    final static String BUY = "BUY";
    
    final static String SELL = "SELL";
    
    final static String OPEN = "OPEN";
    
    final static String CLOSE = "CLOSE";
    
    final static String CLOSE_TODAY = "CLOSE_TODAY";
    
    final static String CLOSE_YESTERDAY = "CLOSE_YESTERDAY";

    String getOrderId();

    String getInstrumentId();
    
    String getExchangeId();

    Double getPrice();

    Integer getQuantity();

    String getDirection();

    String getTradingDay();

    String getOffset();

    String getState();

    Collection<Trade> getTrades();

    Transaction getTransaction();
}
