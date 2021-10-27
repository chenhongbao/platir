package io.platir.engine.core;

import io.platir.Account;
import io.platir.Strategy;
import io.platir.User;
import io.platir.broker.MarketDataService;
import io.platir.broker.TradingService;
import io.platir.engine.AddAcountException;
import io.platir.engine.AddStrategyException;
import io.platir.engine.AddUserException;
import io.platir.engine.InitializeEngineException;
import io.platir.engine.PlatirEngine;
import io.platir.engine.RemoveAcountException;
import io.platir.engine.RemoveStrategyException;
import io.platir.engine.RemoveUserException;
import io.platir.engine.RunStrategyException;
import io.platir.engine.StopStrategyException;
import io.platir.engine.rule.AccountRule;
import io.platir.engine.rule.GlobalRule;
import io.platir.engine.rule.StrategyRule;
import io.platir.engine.rule.UserRule;
import io.platir.user.UserStrategy;
import java.util.Set;

public class PlatirEngineCore extends PlatirEngine {
    
    private final UserManager userManager = new UserManager();
    private final UserStrategyManager userStrategyManager = new UserStrategyManager();

    @Override
    public void setUseService(TradingService tradingService) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void setUseService(MarketDataService marketDataService) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void initialize(GlobalRule globalRule) throws InitializeEngineException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Set<User> getUsers() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public User addUser(String userId, String password, UserRule userRule) throws AddUserException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void removeUser(String userId) throws RemoveUserException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Account addAccount(Double initialBalance, User user, AccountRule accountRule) throws AddAcountException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void removeAccount(String accountId, User user) throws RemoveAcountException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Strategy addStrategy(UserStrategy userStrategy, Account account, StrategyRule strategyRule) throws AddStrategyException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void runStrategy(Strategy strategy) throws RunStrategyException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void stopStrategy(String strategyId) throws StopStrategyException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void removeStrategy(String strategyId) throws RemoveStrategyException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

}
