package io.platir.broker;

public interface MarketDataSnapshot {

    String getInstrumentId();

    String getExchangeId();

    Double getLastPrice();

    Double getAskPrice();

    Double getBidPrice();

    Integer getAskVolume();

    Integer getBidVolume();

    Integer getTotalVolume();

    Integer getOpenInterest();

    Double getOpenPrice();

    Double getClosePrice();

    Double getSettlementPrice();

    String getUpdateTime();

    Double getHighPrice();

    Double getLowPrice();

    String getTradingDay();
}
