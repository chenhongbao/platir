package io.platir;

public interface Instrument {

    String getInstrumentId();

    String getExchangeId();

    Double getMultiple();

    Double getMarginByAmount();

    Double getMarginByQuantity();

    Double getCommissionByAmount();

    Double getCommissionByQuantity();

    String getUpdateTime();
}
