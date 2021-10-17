package io.platir.service;

public interface Account {

    String getAccountId();

    String getUserId();

    void setAccountId(String accountId);

    void setUserId(String userId);

    Double getOpeningMargin();

    void setOpeningMargin(Double openingMargin);

    Double getOpeningCommission();

    void setOpeningCommission(Double openingCommission);

    Double getClosingCommission();

    void setClosingCommission(Double closingCommission);

    Double getBalance();

    void setBalance(Double balance);

    Double getMargin();

    void setMargin(Double margin);

    Double getCommission();

    void setCommission(Double commission);

    Double getAvailable();

    void setAvailable(Double available);

    Double getPositionProfit();

    void setPositionProfit(Double positionProfit);

    Double getCloseProfit();

    void setCloseProfit(Double closeProfit);

    Double getYdBalance();

    void setYdBalance(Double ydBalance);

    String getTradingDay();

    void setTradingDay(String tradingDay);

    String getSettleTime();

    void setSettleTime(String settleTime);

}
