package io.platir;

import java.util.Collection;

public interface Transaction {

    final static String ALL_TRADED = "COMPLETED";

    final static String EXECUTING = "EXECUTING";
    
    final static String QUEUEING = "PENDING";

    final static String RJECTED = "REJECTED";

    String getState();

    void setState(String state);

    String getTransactionId();

    void setTransactionId(String transactionId);

    String getInstrumentId();

    void setInstrumentId(String instrumentId);

    Double getPrice();

    void setPrice(Double price);

    Integer getQuantity();

    void setQuantity(Integer volume);

    String getDirection();

    void setDirection(String direction);

    String getTradingDay();

    void setTradingDay(String tradingDay);

    String getUpdateTime();

    void setUpdateTime(String updateTime);

    String getOffset();

    void setOffset(String offset);

    Collection<Order> getOrders();

    void addOrder(Order order);

    Strategy getStrategy();
}
