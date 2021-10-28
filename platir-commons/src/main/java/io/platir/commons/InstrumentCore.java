package io.platir.commons;

import io.platir.Instrument;

public class InstrumentCore implements Instrument {

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

    public void setInstrumentId(String instrumentId) {
        this.instrumentId = instrumentId;
    }

    @Override
    public String getExchangeId() {
        return exchangeId;
    }

    public void setExchangeId(String exchangeId) {
        this.exchangeId = exchangeId;
    }

    @Override
    public Double getMultiple() {
        return multiple;
    }

    public void setMultiple(Double multiple) {
        this.multiple = multiple;
    }

    @Override
    public Double getCommissionByQuantity() {
        return commissionByQuantity;
    }

    public void setCommissionByQuantity(Double commissionByQuantity) {
        this.commissionByQuantity = commissionByQuantity;
    }

    @Override
    public Double getCommissionByAmount() {
        return commissionByAmount;
    }

    public void setCommissionByAmount(Double commissionByAmount) {
        this.commissionByAmount = commissionByAmount;
    }

    @Override
    public Double getMarginByQuantity() {
        return marginByQuantity;
    }

    public void setMarginByQuantity(Double marginByQuantity) {
        this.marginByQuantity = marginByQuantity;
    }

    @Override
    public Double getMarginByAmount() {
        return marginByAmount;
    }

    public void setMarginByAmount(Double marginByAmount) {
        this.marginByAmount = marginByAmount;
    }

    @Override
    public String getUpdateDatetime() {
        return updateDatetime;
    }

    public void setUpdateDatetime(String datetime) {
        this.updateDatetime = datetime;
    }

}
