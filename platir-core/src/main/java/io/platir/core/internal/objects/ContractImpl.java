package io.platir.core.internal.objects;

import io.platir.service.Contract;

/**
 *
 * @author Chen Hongbao
 */
class ContractImpl implements Contract {

    private String contractId;
    private String userId;
    private String instrumentId;
    private String direction;
    private Double price;
    private Double closePrice;
    private String state;
    private String openTradingDay;
    private String openTime;

    @Override
    public String getContractId() {
        return contractId;
    }

    @Override
    public void setContractId(String contractId) {
        this.contractId = contractId;
    }

    @Override
    public String getUserId() {
        return userId;
    }

    @Override
    public void setUserId(String userId) {
        this.userId = userId;
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
    public String getDirection() {
        return direction;
    }

    @Override
    public void setDirection(String direction) {
        this.direction = direction;
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
    public Double getClosePrice() {
        return closePrice;
    }

    @Override
    public void setClosePrice(Double closePrice) {
        this.closePrice = closePrice;
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
    public String getOpenTradingDay() {
        return openTradingDay;
    }

    @Override
    public void setOpenTradingDay(String openTradingDay) {
        this.openTradingDay = openTradingDay;
    }

    @Override
    public String getOpenTime() {
        return openTime;
    }

    @Override
    public void setOpenTime(String openTime) {
        this.openTime = openTime;
    }

}
