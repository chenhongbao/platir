package io.platir.commons;

import com.google.gson.annotations.Expose;
import io.platir.Trade;

public class TradeCore implements Trade {

    private String tradeId;
    private String instrumentId;
    private String exchangeId;
    private Double price;
    private Integer quantity;
    private String direction;
    private String offset;
    private String tradingDay;
    private String updateDatetime;

    @Expose(serialize = false, deserialize = false)
    private OrderCore order;

    @Override
    public String getExchangeId() {
        return exchangeId;
    }

    public void setExchangeId(String exchangeId) {
        this.exchangeId = exchangeId;
    }

    @Override
    public String getTradeId() {
        return tradeId;
    }

    public void setTradeId(String tradeId) {
        this.tradeId = tradeId;
    }

    @Override
    public String getInstrumentId() {
        return instrumentId;
    }

    public void setInstrumentId(String instrumentId) {
        this.instrumentId = instrumentId;
    }

    @Override
    public Double getPrice() {
        return price;
    }

    public void setPrice(Double price) {
        this.price = price;
    }

    @Override
    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    @Override
    public String getDirection() {
        return direction;
    }

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

    public void setTradingDay(String tradingDay) {
        this.tradingDay = tradingDay;
    }

    @Override
    public String getUpdateDatetime() {
        return updateDatetime;
    }

    public void setUpdateDatetime(String datetime) {
        this.updateDatetime = datetime;
    }

    @Override
    public OrderCore getOrder() {
        return order;
    }

    public void setOrder(OrderCore order) {
        this.order = order;
    }

}
