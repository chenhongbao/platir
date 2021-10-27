package io.platir.engine;

import io.platir.engine.rule.StrategySetting;
import io.platir.engine.rule.UserSetting;
import io.platir.engine.rule.AccountSetting;
import io.platir.Account;
import io.platir.Strategy;
import io.platir.User;
import io.platir.broker.MarketDataService;
import io.platir.broker.TradingService;
import io.platir.engine.core.PlatirEngineCore;
import io.platir.engine.rule.GlobalSetting;
import io.platir.user.UserStrategy;
import java.util.Set;

public abstract class PlatirEngine {

    public static PlatirEngine newEngine() {
        return new PlatirEngineCore();
    }

    public abstract void setUseService(TradingService tradingService);

    public abstract void setUseService(MarketDataService marketDataService);

    public abstract void initialize(GlobalSetting globalRule) throws InitializeEngineException;

    public abstract Set<User> getUsers();

    public abstract User addUser(String userId, String password, UserSetting userRule) throws AddUserException;

    public abstract Account addAccount(Double initialBalance, User user, AccountSetting accountRule) throws AddAccountException;

    public abstract void removeAccount(String accountId, User user) throws RemoveAccountException;

    public abstract Strategy addStrategy(UserStrategy userStrategy, Account account, StrategySetting strategyRule) throws AddStrategyException;

    public abstract void unblockStrategy(Strategy strategy) throws RunStrategyException;

    public abstract void blockStrategy(Strategy strategy) throws StopStrategyException;

    public abstract void removeStrategy(Strategy strategy) throws RemoveStrategyException;
}
