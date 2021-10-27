package io.platir;

import java.util.Collection;

public interface Account {

    String getAccountId();

    Double getOpeningMargin();

    Double getOpeningCommission();

    Double getClosingCommission();

    Double getBalance();

    Double getMargin();

    Double getCommission();

    Double getAvailable();

    Double getPositionProfit();

    Double getCloseProfit();

    Double getYdBalance();

    String getTradingDay();

    String getSettleDatetime();

    Collection<Strategy> getStrategies();
    
    Collection<Contract> getContracts();
    
    User getUser();
}
