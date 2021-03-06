package io.platir.engine.core;

import io.platir.commons.SettingFactoryCore;
import io.platir.commons.StrategyCore;
import io.platir.Account;
import io.platir.Strategy;
import io.platir.User;
import io.platir.broker.MarketDataService;
import io.platir.broker.TradingService;
import io.platir.commons.GlobalSettingCore;
import io.platir.setting.AccountSetting;
import io.platir.engine.AddAccountException;
import io.platir.engine.AddStrategyException;
import io.platir.engine.AddUserException;
import io.platir.setting.GlobalSetting;
import io.platir.engine.InitializeEngineException;
import io.platir.engine.PlatirEngine;
import io.platir.engine.RemoveAccountException;
import io.platir.engine.RemoveStrategyException;
import io.platir.engine.RunStrategyException;
import io.platir.setting.SettingFactory;
import io.platir.engine.StopStrategyException;
import io.platir.engine.timer.EngineTimer;
import io.platir.setting.StrategySetting;
import io.platir.setting.UserSetting;
import io.platir.user.UserStrategy;
import io.platir.utils.Utils;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

public class PlatirEngineCore extends PlatirEngine {

    private final EngineTimer engineTimer = EngineTimer.newTimer();
    private final UserManager userManager = new UserManager();
    private final UserStrategyManager userStrategyManager = new UserStrategyManager();
    private final Map<String, String> tradingServiceParameters = new HashMap<>();
    private final Map<String, String> marketDataServiceParameters = new HashMap<>();

    private TradingAdapter tradingAdapter;
    private MarketDataAdapter marketDataAdapter;
    private TradingService tradingService;
    private MarketDataService marketDataService;
    private GlobalSettingCore globalSetting;

    private static final Logger engineLogger = Logger.getLogger(PlatirEngineCore.class.getSimpleName());
    private static final ExecutorService threads = Executors.newCachedThreadPool();

    static ExecutorService threads() {
        return threads;
    }

    static Logger logger() {
        return engineLogger;
    }

    GlobalSettingCore getGlobalSetting() {
        return globalSetting;
    }

    UserManager getUserManager() {
        return userManager;
    }

    UserStrategyManager getUserStrategyManager() {
        return userStrategyManager;
    }

    TradingAdapter getTradingAdapter() {
        return tradingAdapter;
    }

    UserSession createSession(StrategyCore strategy) {
        return new UserSession(strategy, tradingAdapter, marketDataAdapter, userStrategyManager.getLoggingManager().getLoggingHandler(strategy));
    }

    @Override
    public SettingFactory getSettingFactory() {
        return new SettingFactoryCore();
    }

    @Override
    public void setUseService(TradingService tradingService, Map<String, String> parameters) {
        if (tradingService != null) {
            this.tradingService = tradingService;
        }
        if (parameters != null) {
            tradingServiceParameters.putAll(parameters);
        }
    }

    @Override
    public void setUseService(MarketDataService marketDataService, Map<String, String> parameters) {
        if (marketDataService != null) {
            this.marketDataService = marketDataService;
        }
        if (parameters != null) {
            marketDataServiceParameters.putAll(parameters);
        }
    }

    @Override
    public void initialize(GlobalSetting globalSetting) throws InitializeEngineException {
        globalSetting = new GlobalSettingCore((GlobalSettingCore) globalSetting);
        try {
            if (!globalSetting.isInitialDefered()) {
                initializeNow();
            }
        } finally {
            engineTimer.addJob(new ReinitEngineJob(this));
            engineTimer.addJob(new ClearEngineJob(this));
            logger().addHandler(new LoggingDispatcher(globalSetting.getLoggingListeners()));
            /* Can't initialize twice. */
            lockInstance();
        }
    }

    private void lockInstance() {
        File file = Utils.file(Paths.get(Commons.instanceDirectory().toString(), ".lock"));
        try {
            new FileOutputStream(file).getChannel().lock();
        } catch (IOException exception) {
            logger().log(Level.WARNING, "Can''t lock file for singleton instance. {0}", exception.getMessage());
        }
    }

    void initializeNow() throws InitializeEngineException {
        int returnCode = tradingService.initialize(tradingServiceParameters);
        if (returnCode != 0) {
            throw new InitializeEngineException("Initializing trading service returns " + returnCode + ".");
        } else {
            InfoCenter.setTradingDay(tradingService.getTradingDay());
        }
        returnCode = marketDataService.initialize(tradingServiceParameters);
        if (returnCode != 0) {
            throw new InitializeEngineException("Initializing market data service returns " + returnCode + ".");
        }
        tradingAdapter = new TradingAdapter(tradingService, userStrategyManager.getLookup());
        marketDataAdapter = new MarketDataAdapter(marketDataService, userStrategyManager.getLookup(), globalSetting.isMarketDataParallel());
    }

    @Override
    public Set<User> getUsers() {
        Set<User> users = new HashSet<>();
        userManager.getUsers().forEach(userCore -> users.add(userCore));
        return users;
    }

    @Override
    public User addUser(String userId, String password, UserSetting userSetting) throws AddUserException {
        if (!userSetting.settlementTime().hasValue()) {
            throw new AddUserException("No settlement time setting.");
        }
        var userCore = userManager.addUser(userId, password, userSetting);
        engineTimer.addJob(new SettleUserJob(userCore, this));
        return userCore;
    }

    @Override
    public Account addAccount(Double initialBalance, User user, AccountSetting accountSetting) throws AddAccountException {
        return userManager.addAccount(initialBalance, user, accountSetting);
    }

    @Override
    public void removeAccount(String accountId, User user) throws RemoveAccountException {
        userManager.removeAccount(accountId, user);
    }

    @Override
    public Strategy addStrategy(UserStrategy userStrategy, Account account, StrategySetting strategySetting) throws AddStrategyException {
        StrategyCore newStrategy = userManager.addStrategy(account, strategySetting);
        userStrategyManager.addUserStrategy(newStrategy, userStrategy);
        engineTimer.addJob(new LoadStrategyJob(newStrategy, this));
        engineTimer.addJob(new ConfiguredStrategyJob(newStrategy, this));
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

}
