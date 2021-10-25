package io.platir.core;

import io.platir.Trade;

class TradeCore implements Trade {

    private String tradeId;
    private String instrumentId;
    private Double price;
    private Integer quantity;
    private String direction;
    private String offset;
    private String tradingDay;
    private String updateTime;
    private OrderCore order;

    @Override
    public String getTradeId() {
        return tradeId;
    }

    void setTradeId(String tradeId) {
        this.tradeId = tradeId;
    }

    @Override
    public String getInstrumentId() {
        return instrumentId;
    }

    void setInstrumentId(String instrumentId) {
        this.instrumentId = instrumentId;
    }

    @Override
    public Double getPrice() {
        return price;
    }

    void setPrice(Double price) {
        this.price = price;
    }

    @Override
    public Integer getQuantity() {
        return quantity;
    }

    void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    @Override
    public String getDirection() {
        return direction;
    }

    void setDirection(String direction) {
        this.direction = direction;
    }

    @Override
    public String getOffset() {
        return offset;
    }

    void setOffset(String offset) {
        this.offset = offset;
    }

    @Override
    public String getTradingDay() {
        return tradingDay;
    }

    void setTradingDay(String tradingDay) {
        this.tradingDay = tradingDay;
    }

    @Override
    public String getUpdateTime() {
        return updateTime;
    }

    void setUpdateTime(String updateTime) {
        this.updateTime = updateTime;
    }

    @Override
    public OrderCore getOrder() {
        return order;
    }

    void setOrder(OrderCore order) {
        this.order = order;
    }

}
