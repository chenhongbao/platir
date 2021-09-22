package io.platir.service;

public interface Trade {

	String getTradeId();

	String getOrderId();

	String getInstrumentId();

	Double getPrice();

	Integer getVolume();

	String getDirection();

	String getTradingDay();

	String getUpdateTime();

}
