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
import io.platir.util.Utils;
import java.util.Set;
import java.util.logging.Level;

public class PlatirEngineCore extends PlatirEngine {

    private final UserManager userManager = new UserManager();
    private final UserStrategyManager userStrategyManager = new UserStrategyManager();
    private final LoggingManager loggingManager = new LoggingManager();

    private TradingAdapter tradingAdapter;
    private MarketDataAdapter marketDataAdapter;
    private TradingService tradingService;
    private MarketDataService marketDataService;

    @Override
    public void setUseService(TradingService tradingService) {
        if (tradingService != null) {
            this.tradingService = tradingService;
        }
    }

    @Override
    public void setUseService(MarketDataService marketDataService) {
        if (marketDataService != null) {
            this.marketDataService = marketDataService;
        }
    }

    @Override
    public void initialize(GlobalRule globalRule) throws InitializeEngineException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Set<User> getUsers() {
        return userManager.getUsers();
    }

    @Override
    public User addUser(String userId, String password, UserRule userRule) throws AddUserException {
        return userManager.addUser(userId, password, userRule);
    }

    @Override
    public void removeUser(String userId) throws RemoveUserException {
        userManager.removeUser(userId);
    }

    @Override
    public Account addAccount(Double initialBalance, User user, AccountRule accountRule) throws AddAcountException {
        return userManager.addAccount(initialBalance, user, accountRule);
    }

    @Override
    public void removeAccount(String accountId, User user) throws RemoveAcountException {
        userManager.removeAccount(accountId, user);
    }

    @Override
    public Strategy addStrategy(UserStrategy userStrategy, Account account, StrategyRule strategyRule) throws AddStrategyException {
        Strategy newStrategy = userManager.addStrategy(account, strategyRule);
        userStrategyManager.addUserStrategy(newStrategy, userStrategy);
        tradingAdapter.registerStrategy(newStrategy);
        marketDataAdapter.registerStrategy(newStrategy);
        callbackOnload(newStrategy);
        return newStrategy;
    }

    @Override
    public void runStrategy(Strategy strategy) throws RunStrategyException {
        tradingAdapter.unblockStrategy(strategy);
        marketDataAdapter.unblockStrategy(strategy);
    }

    @Override
    public void stopStrategy(Strategy strategy) throws StopStrategyException {
        tradingAdapter.blockStrategy(strategy);
        marketDataAdapter.blockStrategy(strategy);
    }

    @Override
    public void removeStrategy(String strategyId) throws RemoveStrategyException {
        Strategy removed = userManager.removeStrategy(strategyId);
        userStrategyManager.removeUserStrategy(removed);
    }

    private void callbackOnload(Strategy strategy) {
        try {
            userStrategyManager.getLookup().find(strategy.getStrategyId())
                    .onLoad(new UserSession((StrategyCore) strategy, tradingAdapter, marketDataAdapter, loggingManager.getLoggingHandler(strategy)));
        } catch (NoSuchUserStrategyException exception) {
            Utils.logger().log(Level.SEVERE, "No user strategy found for strategy {0}.", strategy.getStrategyId());
        }
    }
}
