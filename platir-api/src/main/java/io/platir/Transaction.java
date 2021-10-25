package io.platir;

import java.util.Collection;

public interface Transaction {

    final static String ALL_TRADED = "COMPLETED";

    final static String EXECUTING = "EXECUTING";
    
    final static String QUEUEING = "PENDING";

    final static String RJECTED = "REJECTED";

    String getState();

    String getTransactionId();

    String getInstrumentId();

    Double getPrice();

    Integer getQuantity();

    String getDirection();

    String getTradingDay();

    String getUpdateTime();

    String getOffset();

    Collection<Order> getOrders();

    Strategy getStrategy();
}
