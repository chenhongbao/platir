package io.platir.engine.core;

class InstrumentCore {

    private String instrumentId;
    private String exchangeId;
    private Integer multiple;
    private Double commissionByQuantity;
    private Double commissionByAmount;
    private Double marginByQuantity;
    private Double marginByAmount;
    private String updateTime;

    public String getInstrumentId() {
        return instrumentId;
    }

    public void setInstrumentId(String instrumentId) {
        this.instrumentId = instrumentId;
    }

    public String getExchangeId() {
        return exchangeId;
    }

    public void setExchangeId(String exchangeId) {
        this.exchangeId = exchangeId;
    }

    public Integer getMultiple() {
        return multiple;
    }

    public void setMultiple(Integer multiple) {
        this.multiple = multiple;
    }

    public Double getCommissionByQuantity() {
        return commissionByQuantity;
    }

    public void setCommissionByQuantity(Double commissionByQuantity) {
        this.commissionByQuantity = commissionByQuantity;
    }

    public Double getCommissionByAmount() {
        return commissionByAmount;
    }

    public void setCommissionByAmount(Double commissionByAmount) {
        this.commissionByAmount = commissionByAmount;
    }

    public Double getMarginByQuantity() {
        return marginByQuantity;
    }

    public void setMarginByQuantity(Double marginByQuantity) {
        this.marginByQuantity = marginByQuantity;
    }

    public Double getMarginByAmount() {
        return marginByAmount;
    }

    public void setMarginByAmount(Double marginByAmount) {
        this.marginByAmount = marginByAmount;
    }

    public String getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(String updateTime) {
        this.updateTime = updateTime;
    }

}
