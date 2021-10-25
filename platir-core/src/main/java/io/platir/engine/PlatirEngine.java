package io.platir.engine;

import io.platir.Account;
import io.platir.Strategy;
import io.platir.User;
import io.platir.broker.MarketDataService;
import io.platir.broker.TradingService;
import io.platir.engine.core.PlatirEngineCore;
import io.platir.user.UserStrategy;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;

public abstract class PlatirEngine {
    
    public static PlatirEngine newEngine() {
        return new PlatirEngineCore();
    }

    public abstract Set<TradingService> getTradingServices();

    public abstract Set<MarketDataService> getMarketDataServices();

    public abstract void setUseService(TradingService tradingService);

    public abstract void setUseService(MarketDataService marketDataService);
    
    public abstract void initialize() throws InitializeEngineException;
    
    public abstract Set<User> getUsers();
    
    public abstract User addUser(String userId, String password, UserRule userRule) throws AddUserException;
    
    public abstract void removeUser(String userId) throws RemoveUserException;
    
    public abstract Account addAccount(Double initialBalance, User user, AccountRule accountRule) throws AddAcountException;
    
    public abstract void removeAccount(String accountId, User user) throws RemoveAcountException;
    
    public abstract Strategy addStrategy(UserStrategy userStrategy, Account account, StrategyRule strategyRule) throws AddStrategyException;
    
    public abstract void runStrategy(Strategy strategy) throws RunStrategyException;
    
    public abstract void stopStrategy(String strategyId) throws StopStrategyException;
    
    public abstract void removeStrategy(String strategyId) throws RemoveStrategyException;
}