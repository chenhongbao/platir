package io.platir.queries;

import io.platir.service.Trade;

/**
 *
 * @author Chen Hongbao
 */
public class TradeImpl implements Trade {

    private String tradeId;
    private String orderId;
    private String instrumentId;
    private Double price;
    private Integer volume;
    private String direction;
    private String offset;
    private String tradingDay;
    private String updateTime;

    @Override
    public String getTradeId() {
        return tradeId;
    }

    @Override
    public void setTradeId(String tradeId) {
        this.tradeId = tradeId;
    }

    @Override
    public String getOrderId() {
        return orderId;
    }

    @Override
    public void setOrderId(String orderId) {
        this.orderId = orderId;
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
    public Double getPrice() {
        return price;
    }

    @Override
    public void setPrice(Double price) {
        this.price = price;
    }

    @Override
    public Integer getVolume() {
        return volume;
    }

    public void setVolume(Integer volume) {
        this.volume = volume;
    }

    @Override
    public String getDirection() {
        return direction;
    }

    @Override
    public void setDirection(String direction) {
        this.direction = direction;
    }

    @Override
    public String getOffset() {
        return offset;
    }

    public void setOffset(String offset) {
        this.offset = offset;
    }

    @Override
    public String getTradingDay() {
        return tradingDay;
    }

    @Override
    public void setTradingDay(String tradingDay) {
        this.tradingDay = tradingDay;
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
