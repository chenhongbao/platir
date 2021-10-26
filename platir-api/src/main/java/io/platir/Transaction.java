package io.platir;

import java.util.Collection;

public interface Transaction {

    final static String ALL_TRADED = "COMPLETED";

    final static String EXECUTING = "EXECUTING";
    
    final static String PENDING = "PENDING";

    final static String REJECTED = "REJECTED";

    String getState();

    String getTransactionId();

    String getInstrumentId();
    
    String getExchangeId();

    Double getPrice();

    Integer getQuantity();

    String getDirection();

    String getTradingDay();

    String getUpdateTime();

    String getOffset();

    Collection<Order> getOrders();

    Strategy getStrategy();
}
