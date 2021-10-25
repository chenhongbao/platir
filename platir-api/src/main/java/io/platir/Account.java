package io.platir;

import java.util.Collection;

public interface Account {

    String getAccountId();

    void setAccountId(String accountId);;

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

    Collection<Strategy> getStrategies();
    
    void addStrategy(Strategy strategy);
    
    void removeStrategy(String strategyId);
    
    Collection<Contract> getContracts();
    
    void addContract(Contract contract);
    
    void removeContract(String contractId);
    
    User getUser();
}
