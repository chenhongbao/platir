package io.platir;

public interface Trade {

    String getTradeId();

    void setTradeId(String tradeId);
    
    String getInstrumentId();

    void setInstrumentId(String instrumentId);

    Double getPrice();

    void setPrice(Double price);

    Integer getVolume();

    void setVolume(Integer volume);

    String getDirection();

    void setDirection(String direction);

    String getOffset();

    void setOffset(String offset);

    String getTradingDay();

    void setTradingDay(String tradingDay);

    String getUpdateTime();

    void setUpdateTime(String updateTime);
    
    Order getOrder();
}
