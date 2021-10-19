package io.platir.queries;

import io.platir.service.Account;

/**
 *
 * @author Chen Hongbao
 */
class AccountImpl implements Account {

    private String accountId;
    private String userId;
    private Double balance;
    private Double margin;
    private Double commission;
    private Double openingMargin;
    private Double openingCommission;
    private Double closingCommission;
    private Double available;
    private Double positionProfit;
    private Double closeProfit;
    private Double ydBalance;
    private String tradingDay;
    private String settleTime;

    @Override
    public String getAccountId() {
        return accountId;
    }

    @Override
    public void setAccountId(String accountId) {
        this.accountId = accountId;
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
    public Double getBalance() {
        return balance;
    }

    @Override
    public void setBalance(Double balance) {
        this.balance = balance;
    }

    @Override
    public Double getMargin() {
        return margin;
    }

    @Override
    public void setMargin(Double margin) {
        this.margin = margin;
    }

    @Override
    public Double getCommission() {
        return commission;
    }

    @Override
    public void setCommission(Double commission) {
        this.commission = commission;
    }

    @Override
    public Double getOpeningMargin() {
        return openingMargin;
    }

    @Override
    public void setOpeningMargin(Double openingMargin) {
        this.openingMargin = openingMargin;
    }

    @Override
    public Double getOpeningCommission() {
        return openingCommission;
    }

    @Override
    public void setOpeningCommission(Double openingCommission) {
        this.openingCommission = openingCommission;
    }

    @Override
    public Double getClosingCommission() {
        return closingCommission;
    }

    @Override
    public void setClosingCommission(Double closingCommission) {
        this.closingCommission = closingCommission;
    }

    @Override
    public Double getAvailable() {
        return available;
    }

    @Override
    public void setAvailable(Double available) {
        this.available = available;
    }

    @Override
    public Double getPositionProfit() {
        return positionProfit;
    }

    @Override
    public void setPositionProfit(Double positionProfit) {
        this.positionProfit = positionProfit;
    }

    @Override
    public Double getCloseProfit() {
        return closeProfit;
    }

    @Override
    public void setCloseProfit(Double closeProfit) {
        this.closeProfit = closeProfit;
    }

    @Override
    public Double getYdBalance() {
        return ydBalance;
    }

    @Override
    public void setYdBalance(Double ydBalance) {
        this.ydBalance = ydBalance;
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
    public String getSettleTime() {
        return settleTime;
    }

    @Override
    public void setSettleTime(String settleTime) {
        this.settleTime = settleTime;
    }

}
