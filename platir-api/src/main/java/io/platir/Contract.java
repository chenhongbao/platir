package io.platir;

public interface Contract {

    final static String OPENING = "OPENING";

    final static String OPEN = "OPEN";

    final static String CLOSING = "CLOSING";

    final static String CLOSED = "CLOSED";
    
    final static String ABANDONED = "ABANDONED";

    String getContractId();

    void setContractId(String contractId);

    String getAccountId();

    void setAccountId(String accountId);

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
