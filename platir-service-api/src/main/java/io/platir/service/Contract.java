package io.platir.service;

public interface Contract {

    String getContractId();

    void setContractId(String contractId);

    String getUserId();

    void setUserId(String userId);

    String getInstrumentId();

    void setInstrumentId(String instrumentId);

    String getDirection();

    void setDirection(String direction);

    Double getPrice();

    void setPrice(Double price);

    String getState();

    void setState(String state);

    String getOpenTradingDay();

    void setOpenTradingDay(String openTradingDay);

    String getOpenTime();

    void setOpenTime(String openTime);

    Double getClosePrice();

    void setClosePrice(Double closePrice);

    String getSettlementTradingDay();

    void setSettlementTradingDay(String settlementTradingDay);

}
