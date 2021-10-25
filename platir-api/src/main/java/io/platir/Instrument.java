package io.platir;

public interface Instrument {

    String getInstrumentId();

    void setInstrumentId(String instrumentId);

    String getExchangeId();

    void setExchangeId(String exchangeId);

    Double getMultiple();

    void setMultiple(Double multiple);

    Double getAmountMargin();

    void setAmountMargin(Double amountMargin);

    Double getVolumeMargin();

    void setVolumeMargin(Double volumeMargin);

    Double getAmountCommission();

    void setAmountCommission(Double amountCommission);

    Double getVolumeCommission();

    void setVolumeCommission(Double volumeCommission);

    String getUpdateTime();

    void setUpdateTime(String updateTime);

}
