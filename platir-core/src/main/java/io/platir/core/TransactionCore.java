package io.platir.core;

import io.platir.Order;
import io.platir.Transaction;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

class TransactionCore implements Transaction {

    private String state;
    private String transactionId;
    private String instrumentId;
    private Double price;
    private Integer quantity;
    private String direction;
    private String tradingDay;
    private String updateTime;
    private String offset;
    private final Map<String, OrderCore> orderMap = new HashMap<>();
    private StrategyCore strategy;

    @Override
    public String getState() {
        return state;
    }

    @Override
    public Collection<Order> getOrders() {
        return orderMap.values().stream().map(core -> {
            return (Order) core;
        }).collect(Collectors.toSet());
    }

    void setState(String state) {
        this.state = state;
    }

    @Override
    public String getTransactionId() {
        return transactionId;
    }

    void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
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
    public String getOffset() {
        return offset;
    }

    void setOffset(String offset) {
        this.offset = offset;
    }

    @Override
    public StrategyCore getStrategy() {
        return strategy;
    }
    
    Map<String, OrderCore> orderMap() {
        return orderMap;
    }

    void setStrategy(StrategyCore strategy) {
        this.strategy = strategy;
    }

}
