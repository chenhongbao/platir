package io.platir.engine.core;

import io.platir.commons.UserSettingCore;
import io.platir.commons.StrategySettingCore;
import io.platir.commons.AccountSettingCore;
import io.platir.commons.AccountCore;
import io.platir.commons.StrategyCore;
import io.platir.commons.UserCore;
import io.platir.Account;
import io.platir.Contract;
import io.platir.Strategy;
import io.platir.Transaction;
import io.platir.User;
import io.platir.setting.AccountSetting;
import io.platir.engine.AddAccountException;
import io.platir.engine.AddStrategyException;
import io.platir.engine.AddUserException;
import io.platir.engine.RemoveAccountException;
import io.platir.engine.RemoveStrategyException;
import io.platir.setting.StrategySetting;
import io.platir.setting.UserSetting;
import io.platir.utils.Utils;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

class UserManager {

    private final AtomicInteger accountIdCounter = new AtomicInteger(0);
    private final AtomicInteger strategyIdCounter = new AtomicInteger(0);
    private final Map<String, UserCore> users = new ConcurrentHashMap<>();

    Set<UserCore> getUsers() {
        return new HashSet<>(users.values());
    }

    void reload(Set<UserCore> reloadUsers) {
        users.clear();
        reloadUsers.forEach(user -> {
            user.accounts().values().removeIf(account -> account.getState().equals(Account.REMOVED));
            user.accounts().values().forEach(account -> {
                /* Clear removed accounts and strategies, and done contracts. */
                account.strategies().values().removeIf(strategy -> strategy.getState().equals(Strategy.REMOVED));
                account.strategies().values().forEach(strategy -> {
                    strategy.transactions().clear();
                    /* Restore upward reference. */
                    strategy.setAccount(account);
                });
                account.contracts().values().removeIf(contract -> contract.getState().equals(Contract.ABANDONED) || contract.getState().equals(Contract.CLOSED));
                account.setUser(user);
            });
            users.put(user.getUserId(), user);
        });
    }

    private UserCore computeUser(String userId, String password, UserSetting userSetting) {
        var userCore = new UserCore();
        userCore.setCreateDatetime(Utils.datetime());
        userCore.setUserId(userId);
        userCore.setPassword(password);
        userCore.setUserSetting((UserSettingCore) userSetting);
        users.put(userId, userCore);
        return userCore;
    }

    UserCore addUser(String userId, String password, UserSetting userSetting) throws AddUserException {
        if (users.containsKey(userId)) {
            throw new AddUserException("Duplicated user ID " + userId + ".");
        }
        return computeUser(userId, password, userSetting);
    }

    private AccountCore computeAccount(Double initialBalance, UserCore userCore, AccountSettingCore accountSetting) {
        var accountCore = new AccountCore();
        accountCore.setAccountId(userCore.getUserId() + "-" + accountIdCounter.incrementAndGet());
        accountCore.setAccountRule(accountSetting);
        accountCore.setAvailable(initialBalance);
        accountCore.setBalance(initialBalance);
        accountCore.setCloseProfit(0D);
        accountCore.setClosingCommission(0D);
        accountCore.setCommission(0D);
        accountCore.setMargin(0D);
        accountCore.setOpeningCommission(0D);
        accountCore.setOpeningMargin(0D);
        accountCore.setPositionProfit(0D);
        accountCore.setUser(userCore);
        accountCore.setYdBalance(0D);
        userCore.accounts().put(accountCore.getAccountId(), accountCore);
        return accountCore;
    }

    AccountCore addAccount(Double initialBalance, User user, AccountSetting accountSetting) throws AddAccountException {
        var userCore = (UserCore) user;
        var userRule = userCore.getUserSetting();
        if (!userRule.maxAccountCount().check(userCore.getAccounts().size() + 1)) {
            throw new AddAccountException("Account number under user(" + userCore.getUserId() + ") exceeds limit " + userRule.maxAccountCount().get() + ".");
        }
        if (!userRule.maxInitialBalance().check(initialBalance)) {
            throw new AddAccountException("Initial account balance under user(" + userCore.getUserId() + ") exceeds limit " + userRule.maxInitialBalance().get() + ".");
        }
        return computeAccount(initialBalance, userCore, (AccountSettingCore) accountSetting);
    }

    private boolean isAccountRemovable(AccountCore accountCore) {
        Boolean strategyDone = accountCore.strategies().values().stream()
                .filter(strategy -> {
                    return !strategy.getState().equals(Strategy.REMOVED);
                }).count() == 0;
        Boolean contractDone = accountCore.contracts().values().stream()
                .filter(contract -> !contract.getState().equals(Contract.CLOSED) && !contract.getState().equals(Contract.ABANDONED))
                .count() == 0;
        return strategyDone && contractDone;
    }

    AccountCore removeAccount(String accountId, User user) throws RemoveAccountException {
        var userCore = (UserCore) user;
        var accountCore = userCore.accounts().get(accountId);
        if (accountCore == null) {
            throw new RemoveAccountException("No such account(" + accountId + ") under user(" + user.getUserId() + ").");
        }
        synchronized (accountCore.syncObject()) {
            if (!isAccountRemovable(accountCore)) {
                throw new RemoveAccountException("Account(" + accountId + ") can't be removed due to incompleted strategies or contracts.");
            }
            accountCore.setState(Account.REMOVED);
        }
        return accountCore;
    }

    private StrategyCore computeStrategy(AccountCore accountCore, StrategySetting strategySetting) {
        var strategyCore = new StrategyCore();
        strategyCore.setAccount(accountCore);
        strategyCore.setCreateDatetime(Utils.date());
        strategyCore.setState(Strategy.NORMAL);
        strategyCore.setStrategySetting((StrategySettingCore) strategySetting);
        strategyCore.setStrategyId(Utils.date() + "-" + strategyIdCounter.incrementAndGet());
        synchronized (accountCore.syncObject()) {
            accountCore.strategies().put(strategyCore.getStrategyId(), strategyCore);
        }
        return strategyCore;
    }

    StrategyCore addStrategy(Account account, StrategySetting strategySetting) throws AddStrategyException {
        var accountCore = (AccountCore) account;
        var rule = accountCore.getAccountSetting().maxStrategyCount();
        if (!rule.check(account.getStrategies().size() + 1)) {
            throw new AddStrategyException("User strategies number under account(" + accountCore.getAccountId() + ") exceeds limit " + rule.get() + ".");
        }
        return computeStrategy(accountCore, strategySetting);
    }

    private void checkStrategyRemovable(StrategyCore strategyCore) throws RemoveStrategyException {
        Set<Transaction> aliveTransactions = strategyCore.transactions().values().stream()
                .filter(transaction -> {
                    return transaction.getState().equals(Transaction.EXECUTING) || transaction.getState().equals(Transaction.PENDING);
                }).collect(Collectors.toSet());
        if (aliveTransactions.size() > 0) {
            var iterator = aliveTransactions.iterator();
            String message = iterator.next().getTransactionId();
            while (iterator.hasNext()) {
                message += ", " + iterator.next().getTransactionId();
            }
            throw new RemoveStrategyException("Strategy(" + strategyCore.getStrategyId() + ") can't be removed because transactions(" + message + ") are alive.");
        }
    }

    StrategyCore removeStrategy(String strategyId, Account account) throws RemoveStrategyException {
        var accountCore = (AccountCore) account;
        var strategyCore = accountCore.strategies().get(strategyId);
        if (strategyCore == null) {
            throw new RemoveStrategyException("No such strategy(" + strategyId + ") under account(" + account.getAccountId() + ").");
        }
        synchronized (accountCore.syncObject()) {
            checkStrategyRemovable(strategyCore);
            strategyCore.setState(Strategy.REMOVED);
            return strategyCore;
        }
    }

}
