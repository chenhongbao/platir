package io.platir.service;

public interface Transaction {

    String getState();

    void setState(String state);

    String getStateMessage();

    void setStateMessage(String stateMessage);

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

    String getUpdateTime();

    void setUpdateTime(String updateTime);

    String getOffset();

    void setOffset(String offset);

    String getStrategyId();

    void setStrategyId(String strategyId);

}
