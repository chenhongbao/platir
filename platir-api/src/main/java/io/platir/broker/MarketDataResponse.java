package io.platir.broker;

public interface MarketDataResponse {

    final static String MARKETDATA_SNAPSHOT_ONLY = "MARKETDATA_SNAPSHOT_ONLY";

    final static String BAR_ONLY = "BAR_ONLY";

    final static String FULL = "FULL";

    String getInstrumentId();

    String getExchangeId();

    String getMarketDateType();
}
