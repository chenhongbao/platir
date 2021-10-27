package io.platir.engine.core;

import io.platir.Instrument;

class InstrumentCore implements Instrument {

    private String instrumentId;
    private String exchangeId;
    private Double multiple;
    private Double commissionByQuantity;
    private Double commissionByAmount;
    private Double marginByQuantity;
    private Double marginByAmount;
    private String updateDatetime;

    @Override
    public String getInstrumentId() {
        return instrumentId;
    }

    void setInstrumentId(String instrumentId) {
        this.instrumentId = instrumentId;
    }

    @Override
    public String getExchangeId() {
        return exchangeId;
    }

    void setExchangeId(String exchangeId) {
        this.exchangeId = exchangeId;
    }

    @Override
    public Double getMultiple() {
        return multiple;
    }

    void setMultiple(Double multiple) {
        this.multiple = multiple;
    }

    @Override
    public Double getCommissionByQuantity() {
        return commissionByQuantity;
    }

    void setCommissionByQuantity(Double commissionByQuantity) {
        this.commissionByQuantity = commissionByQuantity;
    }

    @Override
    public Double getCommissionByAmount() {
        return commissionByAmount;
    }

    void setCommissionByAmount(Double commissionByAmount) {
        this.commissionByAmount = commissionByAmount;
    }

    @Override
    public Double getMarginByQuantity() {
        return marginByQuantity;
    }

    void setMarginByQuantity(Double marginByQuantity) {
        this.marginByQuantity = marginByQuantity;
    }

    @Override
    public Double getMarginByAmount() {
        return marginByAmount;
    }

    void setMarginByAmount(Double marginByAmount) {
        this.marginByAmount = marginByAmount;
    }

    @Override
    public String getUpdateDatetime() {
        return updateDatetime;
    }

    void setUpdateDatetime(String datetime) {
        this.updateDatetime = datetime;
    }

}
