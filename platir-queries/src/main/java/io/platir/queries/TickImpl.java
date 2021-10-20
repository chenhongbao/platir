package io.platir.queries;

import io.platir.service.Tick;

/**
 *
 * @author Chen Hongbao
 */
public class TickImpl implements Tick {

    private String tickId;
    private String instrumentId;
    private Double lastPrice;
    private Double askPrice;
    private Double bidPrice;
    private Integer askVolume;
    private Integer bidVolume;
    private Integer todayVolume;
    private Integer openInterest;
    private Double openPrice;
    private Double closePrice;
    private Double settlementPrice;
    private String updateTime;

    @Override
    public String getTickId() {
        return tickId;
    }

    @Override
    public void setTickId(String tickId) {
        this.tickId = tickId;
    }

    @Override
    public String getInstrumentId() {
        return instrumentId;
    }

    @Override
    public void setInstrumentId(String instrumentId) {
        this.instrumentId = instrumentId;
    }

    @Override
    public Double getLastPrice() {
        return lastPrice;
    }

    public void setLastPrice(Double lastPrice) {
        this.lastPrice = lastPrice;
    }

    @Override
    public Double getAskPrice() {
        return askPrice;
    }

    @Override
    public void setAskPrice(Double askPrice) {
        this.askPrice = askPrice;
    }

    @Override
    public Double getBidPrice() {
        return bidPrice;
    }

    @Override
    public void setBidPrice(Double bidPrice) {
        this.bidPrice = bidPrice;
    }

    @Override
    public Integer getAskVolume() {
        return askVolume;
    }

    @Override
    public void setAskVolume(Integer askVolume) {
        this.askVolume = askVolume;
    }

    @Override
    public Integer getBidVolume() {
        return bidVolume;
    }

    @Override
    public void setBidVolume(Integer bidVolume) {
        this.bidVolume = bidVolume;
    }

    @Override
    public Integer getTodayVolume() {
        return todayVolume;
    }

    @Override
    public void setTodayVolume(Integer todayVolume) {
        this.todayVolume = todayVolume;
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
    public Double getOpenPrice() {
        return openPrice;
    }

    @Override
    public void setOpenPrice(Double openPrice) {
        this.openPrice = openPrice;
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
    public Double getSettlementPrice() {
        return settlementPrice;
    }

    @Override
    public void setSettlementPrice(Double settlementPrice) {
        this.settlementPrice = settlementPrice;
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
