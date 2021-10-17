package io.platir.service;

public interface Bar {

    String getInstrumentId();

    void setInstrumentId(String instrumentId);

    Integer getMinute();

    void setMinute(Integer minute);

    Double getOpenPrice();

    void setOpenPrice(Double openPrice);

    Double getHighPrice();

    void setHighPrice(Double highPrice);

    Double getLowPrice();

    void setLowPrice(Double lowPrice);

    Double getClosePrice();

    void setClosePrice(Double closePrice);

    Integer getVolume();

    void setVolume(Integer volume);

    Integer getOpenInterest();

    void setOpenInterest(Integer openInterest);

    String getUpdateTime();

    void setUpdateTime(String updateTime);

}
