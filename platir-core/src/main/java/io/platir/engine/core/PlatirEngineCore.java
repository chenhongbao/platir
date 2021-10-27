package io.platir.engine.core;

import io.platir.Account;
import io.platir.Strategy;
import io.platir.User;
import io.platir.broker.MarketDataService;
import io.platir.broker.TradingService;
import io.platir.engine.AddAccountException;
import io.platir.engine.AddStrategyException;
import io.platir.engine.AddUserException;
import io.platir.engine.InitializeEngineException;
import io.platir.engine.PlatirEngine;
import io.platir.engine.RemoveAccountException;
import io.platir.engine.RemoveStrategyException;
import io.platir.engine.RunStrategyException;
import io.platir.engine.StopStrategyException;
import io.platir.engine.rule.AccountSetting;
import io.platir.engine.rule.GlobalSetting;
import io.platir.engine.rule.StrategySetting;
import io.platir.engine.rule.UserSetting;
import io.platir.user.UserStrategy;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

public class PlatirEngineCore extends PlatirEngine {

    private final UserManager userManager = new UserManager();
    private final UserStrategyManager userStrategyManager = new UserStrategyManager();

    private TradingAdapter tradingAdapter;
    private MarketDataAdapter marketDataAdapter;
    private TradingService tradingService;
    private MarketDataService marketDataService;

    private static final Logger engineLogger = Logger.getLogger(PlatirEngineCore.class.getSimpleName());
    private static final ExecutorService threads = Executors.newCachedThreadPool();

    static ExecutorService threads() {
        return threads;
    }

    static Logger logger() {
        return engineLogger;
    }

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
    public void initialize(GlobalSetting globalRule) throws InitializeEngineException {
        // TODO Implement initialize() method of PlatirEngineCore.
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Set<User> getUsers() {
        return userManager.getUsers();
    }

    @Override
    public User addUser(String userId, String password, UserSetting userRule) throws AddUserException {
        return userManager.addUser(userId, password, userRule);
    }

    @Override
    public Account addAccount(Double initialBalance, User user, AccountSetting accountRule) throws AddAccountException {
        return userManager.addAccount(initialBalance, user, accountRule);
    }

    @Override
    public void removeAccount(String accountId, User user) throws RemoveAccountException {
        userManager.removeAccount(accountId, user);
    }

    @Override
    public Strategy addStrategy(UserStrategy userStrategy, Account account, StrategySetting strategyRule) throws AddStrategyException {
        StrategyCore newStrategy = userManager.addStrategy(account, strategyRule);
        userStrategyManager.addUserStrategy(newStrategy, userStrategy);
        callbackOnload(newStrategy);
        return newStrategy;
    }

    @Override
    public void unblockStrategy(Strategy strategy) throws RunStrategyException {
        var core = ((StrategyCore) strategy);
        synchronized (core.syncObject()) {
            core.setState(Strategy.NORMAL);
        }
    }

    @Override
    public void blockStrategy(Strategy strategy) throws StopStrategyException {
        var core = ((StrategyCore) strategy);
        synchronized (core.syncObject()) {
            core.setState(Strategy.BLOCKED);
        }
    }

    @Override
    public void removeStrategy(Strategy strategy) throws RemoveStrategyException {
        userStrategyManager.removeUserStrategy(userManager.removeStrategy(strategy.getStrategyId(), strategy.getAccount()));
    }

    private void callbackOnload(StrategyCore strategy) {
        try {
            userStrategyManager.getLookup().findStrategy(strategy)
                    .onLoad(new UserSession((StrategyCore) strategy, tradingAdapter, marketDataAdapter, userStrategyManager.getLoggingManager().getLoggingHandler(strategy)));
        } catch (NoSuchUserStrategyException exception) {
            logger().log(Level.SEVERE, "No user strategy found for strategy {0}.", strategy.getStrategyId());
        }
    }
}
