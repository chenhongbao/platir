package io.platir.service;

public interface Order {

    String getOrderId();

    void setOrderId(String orderId);

    String getTransactionId();

    void setTransactionId(String transactionId);

    String getInstrumentId();

    void setInstrumentId(String instrumentId);

    Double getPrice();

    void setPrice(Double price);

    Integer getVolume();

    void setVolume(Integer volume);

    String getDirection();

    void setDirection(String direction);

    String getTradingDay();

    void setTradingDay(String tradingDay);

    String getOffset();

    void setOffset(String offset);

    String getState();

    void setState(String state);
}
