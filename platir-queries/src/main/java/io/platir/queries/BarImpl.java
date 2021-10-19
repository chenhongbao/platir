package io.platir.queries;

import io.platir.service.Bar;

/**
 *
 * @author Chen Hongbao
 */
class BarImpl implements Bar {

    private String instrumentId;
    private Integer minute;
    private Double openPrice;
    private Double highPrice;
    private Double lowPrice;
    private Double closePrice;
    private Integer volume;
    private Integer openInterest;
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
    public Integer getMinute() {
        return minute;
    }

    @Override
    public void setMinute(Integer minute) {
        this.minute = minute;
    }

    @Override
    public Double getOpenPrice() {
        return openPrice;
    }

    @Override
    public void setOpenPrice(Double openPrice) {
        this.openPrice = openPrice;
    }

    @Override
    public Double getHighPrice() {
        return highPrice;
    }

    @Override
    public void setHighPrice(Double highPrice) {
        this.highPrice = highPrice;
    }

    @Override
    public Double getLowPrice() {
        return lowPrice;
    }

    @Override
    public void setLowPrice(Double lowPrice) {
        this.lowPrice = lowPrice;
    }

    @Override
    public Double getClosePrice() {
        return closePrice;
    }

    @Override
    public void setClosePrice(Double closePrice) {
        this.closePrice = closePrice;
    }

    @Override
    public Integer getVolume() {
        return volume;
    }

    @Override
    public void setVolume(Integer volume) {
        this.volume = volume;
    }

    @Override
    public Integer getOpenInterest() {
        return openInterest;
    }

    @Override
    public void setOpenInterest(Integer openInterest) {
        this.openInterest = openInterest;
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
