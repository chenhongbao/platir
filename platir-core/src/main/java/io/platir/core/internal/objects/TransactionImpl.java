package io.platir.core.internal.objects;

import io.platir.service.Transaction;

/**
 *
 * @author Chen Hongbao
 */
class TransactionImpl implements Transaction {

    private String transactionId;
    private String strategyId;
    private String instrumentId;
    private Double price;
    private Integer volume;
    private String offset;
    private String direction;
    private String state;
    private String stateMessage;
    private String tradingDay;
    private String updateTime;

    @Override
    public String getTransactionId() {
        return transactionId;
    }

    @Override
    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    @Override
    public String getStrategyId() {
        return strategyId;
    }

    @Override
    public void setStrategyId(String strategyId) {
        this.strategyId = strategyId;
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

    @Override
    public void setVolume(Integer volume) {
        this.volume = volume;
    }

    @Override
    public String getOffset() {
        return offset;
    }

    @Override
    public void setOffset(String offset) {
        this.offset = offset;
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
    public String getState() {
        return state;
    }

    @Override
    public void setState(String state) {
        this.state = state;
    }

    @Override
    public String getStateMessage() {
        return stateMessage;
    }

    @Override
    public void setStateMessage(String stateMessage) {
        this.stateMessage = stateMessage;
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
