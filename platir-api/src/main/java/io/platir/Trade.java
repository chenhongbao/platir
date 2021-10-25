package io.platir;

public interface Trade {

    String getTradeId();
    
    String getInstrumentId();

    Double getPrice();

    Integer getQuantity();

    String getDirection();

    String getOffset();

    String getTradingDay();

    String getUpdateTime();
    
    Order getOrder();
}
