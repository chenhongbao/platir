package io.platir.service;

public interface Tick {

	String getInstrumentId();

	Double getLastPrice();

	Double getAskPrice();

	Double getBidPrice();

	Integer getAskVolume();

	Integer getBidVolume();

	Integer getTodayVolume();

	Integer getOpenInterest();

	Double getOpenPrice();

	Double getClosePrice();

	Double getSettlementPrice();

	String getUpdateTime();

}
