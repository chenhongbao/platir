package io.platir.engine.core;

import io.platir.Order;
import io.platir.Trade;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

class OrderCore implements Order {

    private String orderId;
    private String instrumentId;
    private String exchangeId;
    private Double price;
    private Integer quantity;
    private String direction;
    private String tradingDay;
    private String offset;
    private String state;
    private final Map<String, TradeCore> tradeMap = new HashMap<>();
    private TransactionCore transaction;

    @Override
    public String getOrderId() {
        return orderId;
    }

    void setOrderId(String orderId) {
        this.orderId = orderId;
    }

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
    public String getTradingDay() {
        return tradingDay;
    }

    void setTradingDay(String tradingDay) {
        this.tradingDay = tradingDay;
    }

    @Override
    public String getOffset() {
        return offset;
    }

    void setOffset(String offset) {
        this.offset = offset;
    }

    @Override
    public String getState() {
        return state;
    }

    void setState(String state) {
        this.state = state;
    }

    @Override
    public TransactionCore getTransaction() {
        return transaction;
    }

    @Override
    public Collection<Trade> getTrades() {
        return tradeMap.values().stream().map(core -> {
            return (Trade) core;
        }).collect(Collectors.toSet());
    }

    Map<String, TradeCore> tradeMap() {
        return tradeMap;
    }

    void setTransaction(TransactionCore transaction) {
        this.transaction = transaction;
    }

}
