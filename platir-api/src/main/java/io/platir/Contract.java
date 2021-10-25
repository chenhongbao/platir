package io.platir;

public interface Contract {

    final static String OPENING = "OPENING";

    final static String OPEN = "OPEN";

    final static String CLOSING = "CLOSING";

    final static String CLOSED = "CLOSED";
    
    final static String ABANDONED = "ABANDONED";

    String getContractId();

    String getAccountId();

    String getInstrumentId();

    String getDirection();

    Double getPrice();;

    String getState();

    String getOpenTradingDay();

    String getOpenTime();

    Double getClosePrice();

    String getSettlementTradingDay();

}
