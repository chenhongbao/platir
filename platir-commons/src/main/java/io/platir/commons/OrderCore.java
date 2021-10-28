package io.platir.commons;

import com.google.gson.annotations.Expose;
import io.platir.Order;
import io.platir.Trade;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class OrderCore implements Order {

    private String orderId;
    private String instrumentId;
    private String exchangeId;
    private Double price;
    private Integer quantity;
    private String direction;
    private String tradingDay;
    private String offset;
    private String state;
    private final Map<String, TradeCore> trades = new ConcurrentHashMap<>();

    @Expose(serialize = false, deserialize = false)
    private TransactionCore transaction;

    @Override
    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

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
    public String getTradingDay() {
        return tradingDay;
    }

    public void setTradingDay(String tradingDay) {
        this.tradingDay = tradingDay;
    }

    @Override
    public String getOffset() {
        return offset;
    }

    public void setOffset(String offset) {
        this.offset = offset;
    }

    @Override
    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    @Override
    public TransactionCore getTransaction() {
        return transaction;
    }

    @Override
    public Collection<Trade> getTrades() {
        return trades.values().stream().map(core -> {
            return (Trade) core;
        }).collect(Collectors.toSet());
    }

    public Map<String, TradeCore> trades() {
        return trades;
    }

    public void setTransaction(TransactionCore transaction) {
        this.transaction = transaction;
    }

}
