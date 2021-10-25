package io.platir.broker;

public interface Bar {

    String getInstrumentId();

    Integer getMinute();

    Double getOpenPrice();

    ;

    Double getHighPrice();

    Double getLowPrice();

    Double getClosePrice();

    Integer getVolume();

    Integer getOpenInterest();

    String getUpdateTime();

}
