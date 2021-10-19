package io.platir.queries;

import io.platir.service.Instrument;

/**
 *
 * @author Chen Hongbao
 */
class InstrumentImpl implements Instrument {

    private String instrumentId;
    private String exchangeId;
    private Double multiple;
    private Double amountMargin;
    private Double volumeMargin;
    private Double amountCommission;
    private Double volumeCommission;
    private String updateTime;

    @Override
    public String getInstrumentId() {
        return instrumentId;
    }

    @Override
    public void setInstrumentId(String instrumentId) {
        this.instrumentId = instrumentId;
    }

    @Override
    public String getExchangeId() {
        return exchangeId;
    }

    @Override
    public void setExchangeId(String exchangeId) {
        this.exchangeId = exchangeId;
    }

    @Override
    public Double getMultiple() {
        return multiple;
    }

    @Override
    public void setMultiple(Double multiple) {
        this.multiple = multiple;
    }

    @Override
    public Double getAmountMargin() {
        return amountMargin;
    }

    @Override
    public void setAmountMargin(Double amountMargin) {
        this.amountMargin = amountMargin;
    }

    @Override
    public Double getVolumeMargin() {
        return volumeMargin;
    }

    @Override
    public void setVolumeMargin(Double volumeMargin) {
        this.volumeMargin = volumeMargin;
    }

    @Override
    public Double getAmountCommission() {
        return amountCommission;
    }

    @Override
    public void setAmountCommission(Double amountCommission) {
        this.amountCommission = amountCommission;
    }

    @Override
    public Double getVolumeCommission() {
        return volumeCommission;
    }

    @Override
    public void setVolumeCommission(Double volumeCommission) {
        this.volumeCommission = volumeCommission;
    }

    @Override
    public String getUpdateTime() {
        return updateTime;
    }

    @Override
    public void setUpdateTime(String updateTime) {
        this.updateTime = updateTime;
    }

}
