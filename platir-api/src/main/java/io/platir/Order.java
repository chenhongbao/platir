package io.platir;

import java.util.Collection;

public interface Order {

    final static String ALL_TRADED = "ALL_TRADED";

    final static String QUEUEING = "QUEUEING";

    final static String CANCELED = "CANCELED";

    final static String RJECTED = "REJECTED";

    String getOrderId();

    void setOrderId(String orderId);

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

    String getOffset();

    void setOffset(String offset);

    String getState();

    void setState(String state);

    Collection<Trade> getTrades();

    void addTrade(Trade trade);

    Transaction getTransaction();
}
