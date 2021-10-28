package io.platir.commons;

import io.platir.Contract;

public class ContractCore implements Contract {

    private String contractId;
    private String accountId;
    private String instrumentId;
    private String exchangeId;
    private String direction;
    private Double price;
    private String state;
    private String openTradingDay;
    private String openDatetime;
    private Double closePrice;
    private String settlementTradingDay;

    @Override
    public String getContractId() {
        return contractId;
    }

    public void setContractId(String contractId) {
        this.contractId = contractId;
    }

    @Override
    public String getAccountId() {
        return accountId;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
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
    public String getDirection() {
        return direction;
    }

    public void setDirection(String direction) {
        this.direction = direction;
    }

    @Override
    public Double getPrice() {
        return price;
    }

    public void setPrice(Double price) {
        this.price = price;
    }

    @Override
    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    @Override
    public String getOpenTradingDay() {
        return openTradingDay;
    }

    public void setOpenTradingDay(String openTradingDay) {
        this.openTradingDay = openTradingDay;
    }

    @Override
    public String getOpenDatetime() {
        return openDatetime;
    }

    public void setOpenDatetime(String datetime) {
        this.openDatetime = datetime;
    }

    @Override
    public Double getClosePrice() {
        return closePrice;
    }

    public void setClosePrice(Double closePrice) {
        this.closePrice = closePrice;
    }

    @Override
    public String getSettlementTradingDay() {
        return settlementTradingDay;
    }

    public void setSettlementTradingDay(String settlementTradingDay) {
        this.settlementTradingDay = settlementTradingDay;
    }

}
