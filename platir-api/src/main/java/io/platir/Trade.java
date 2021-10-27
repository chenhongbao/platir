package io.platir;

public interface Trade {

    String getTradeId();
    
    String getInstrumentId();
    
    String getExchangeId();

    Double getPrice();

    Integer getQuantity();

    String getDirection();

    String getOffset();

    String getTradingDay();

    String getUpdateTime();
    
    Order getOrder();
}
