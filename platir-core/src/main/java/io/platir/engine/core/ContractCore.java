package io.platir.engine.core;

import io.platir.Contract;

class ContractCore implements Contract {

    private String contractId;
    private String accountId;
    private String instrumentId;
    private String exchangeId;
    private String direction;
    private Double price;
    private String state;
    private String openTradingDay;
    private String openTime;
    private Double closePrice;
    private String settlementTradingDay;

    @Override
    public String getContractId() {
        return contractId;
    }

    void setContractId(String contractId) {
        this.contractId = contractId;
    }

    @Override
    public String getAccountId() {
        return accountId;
    }

    void setAccountId(String accountId) {
        this.accountId = accountId;
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
    public String getDirection() {
        return direction;
    }

    void setDirection(String direction) {
        this.direction = direction;
    }

    @Override
    public Double getPrice() {
        return price;
    }

    void setPrice(Double price) {
        this.price = price;
    }

    @Override
    public String getState() {
        return state;
    }

    void setState(String state) {
        this.state = state;
    }

    @Override
    public String getOpenTradingDay() {
        return openTradingDay;
    }

    void setOpenTradingDay(String openTradingDay) {
        this.openTradingDay = openTradingDay;
    }

    @Override
    public String getOpenTime() {
        return openTime;
    }

    void setOpenTime(String openTime) {
        this.openTime = openTime;
    }

    @Override
    public Double getClosePrice() {
        return closePrice;
    }

    void setClosePrice(Double closePrice) {
        this.closePrice = closePrice;
    }

    @Override
    public String getSettlementTradingDay() {
        return settlementTradingDay;
    }

    void setSettlementTradingDay(String settlementTradingDay) {
        this.settlementTradingDay = settlementTradingDay;
    }

}
