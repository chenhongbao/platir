package io.platir.service;

public interface Tick {

    String getTickId();

    void setTickId(String tickId);

    String getInstrumentId();

    void setInstrumentId(String instrumentId);

    Double getLastPrice();

    void setLastPrice(Double lastPrice);

    Double getAskPrice();

    void setAskPrice(Double askPrice);

    Double getBidPrice();

    void setBidPrice(Double bidPrice);

    Integer getAskVolume();

    void setAskVolume(Integer askVolume);

    Integer getBidVolume();

    void setBidVolume(Integer bidVolume);

    Integer getTodayVolume();

    void setTodayVolume(Integer todayVolume);

    Integer getOpenInterest();

    void setOpenInterest(Integer openInterest);

    Double getOpenPrice();

    void setOpenPrice(Double openPrice);

    Double getClosePrice();

    void setClosePrice(Double closePrice);

    Double getSettlementPrice();

    void setSettlementPrice(Double settlementPrice);

    String getUpdateTime();

    void setUpdateTime(String updateTime);

}
