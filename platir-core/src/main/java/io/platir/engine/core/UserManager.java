package io.platir.engine.core;

import io.platir.Account;
import io.platir.Contract;
import io.platir.Strategy;
import io.platir.Transaction;
import io.platir.User;
import io.platir.engine.AddAccountException;
import io.platir.engine.AddStrategyException;
import io.platir.engine.AddUserException;
import io.platir.engine.RemoveAccountException;
import io.platir.engine.RemoveStrategyException;
import io.platir.engine.RemoveUserException;
import io.platir.engine.rule.AccountSetting;
import io.platir.engine.rule.StrategySetting;
import io.platir.engine.rule.UserSetting;
import io.platir.util.Utils;
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

    Set<User> getUsers() {
        return new HashSet<>(users.values());
    }

    private UserCore computeUser(String userId, String password, UserSetting userRule) {
        var userCore = new UserCore();
        userCore.setCreateDatetime(Utils.datetime());
        userCore.setUserId(userId);
        userCore.setPassword(password);
        userCore.setUserSetting(userRule);
        users.put(userId, userCore);
        return userCore;
    }

    UserCore addUser(String userId, String password, UserSetting userRule) throws AddUserException {
        if (users.containsKey(userId)) {
            throw new AddUserException("Duplicated user ID " + userId + ".");
        }
        return computeUser(userId, password, userRule);
    }

    private void checkUserRemovable(UserCore userCore) throws RemoveUserException {
        var unremovables = userCore.accounts().values().stream()
                .filter(account -> checkAccountRemovable(account))
                .collect(Collectors.toSet());
        if (!unremovables.isEmpty()) {
            var iterator = unremovables.iterator();
            String message = iterator.next().getAccountId();
            while (iterator.hasNext()) {
                message += ", " + iterator.next().getAccountId();
            }
            throw new RemoveUserException("Accounts(" + message + ") can't be removed due to alive strategies or contracts.");
        }
    }

    UserCore removeUser(String userId) throws RemoveUserException {
        var userCore = users.get(userId);
        if (userCore == null) {
            throw new RemoveUserException("No such user(" + userId + ").");
        }
        checkUserRemovable(userCore);
        return users.remove(userId);
    }

    private AccountCore computeAccount(Double initialBalance, UserCore userCore, AccountSetting accountRule) {
        var accountCore = new AccountCore();
        accountCore.setAccountId(userCore.getUserId() + "-" + accountIdCounter.incrementAndGet());
        accountCore.setAccountRule(accountRule);
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

    AccountCore addAccount(Double initialBalance, User user, AccountSetting accountRule) throws AddAccountException {
        var userCore = (UserCore) user;
        var userRule = userCore.getUserSetting();
        if (!userRule.maxAccountCount().check(userCore.getAccounts().size() + 1)) {
            throw new AddAccountException("Account number under user(" + userCore.getUserId() + ") exceeds limit " + userRule.maxAccountCount().get() + ".");
        }
        if (!userRule.maxInitialBalance().check(initialBalance)) {
            throw new AddAccountException("Initial account balance under user(" + userCore.getUserId() + ") exceeds limit " + userRule.maxInitialBalance().get() + ".");
        }
        return computeAccount(initialBalance, userCore, accountRule);
    }

    private boolean checkAccountRemovable(AccountCore accountCore) {
        return accountCore.strategies().values().stream().filter(strategy -> !strategy.getState().equals(Strategy.REMOVED)).count() > 0
                || accountCore.contracts().values().stream().filter(contract -> !contract.getState().equals(Contract.CLOSED) && !contract.getState().equals(Contract.ABANDONED)).count() > 0;
    }

    AccountCore removeAccount(String accountId, User user) throws RemoveAccountException {
        var userCore = (UserCore) user;
        var accountCore = userCore.accounts().get(accountId);
        if (accountCore == null) {
            throw new RemoveAccountException("No such account(" + accountId + ") under user(" + user.getUserId() + ").");
        }
        if (!checkAccountRemovable(accountCore)) {
            throw new RemoveAccountException("Account(" + accountId + ") can't be removed due to incompleted strategies or contracts.");
        }
        return userCore.accounts().remove(accountId);
    }

    private StrategyCore computeStrategy(AccountCore accountCore, StrategySetting strategyRule) {
        var strategyCore = new StrategyCore();
        strategyCore.setAccount(accountCore);
        strategyCore.setCreateDatetime(Utils.date());
        strategyCore.setState(Strategy.NORMAL);
        strategyCore.setStrategySetting(strategyRule);
        strategyCore.setStrategyId(Utils.date() + "-" + strategyIdCounter.incrementAndGet());
        accountCore.strategies().put(strategyCore.getStrategyId(), strategyCore);
        return strategyCore;
    }

    StrategyCore addStrategy(Account account, StrategySetting strategyRule) throws AddStrategyException {
        var accountCore = (AccountCore) account;
        var rule = accountCore.getAccountSetting().maxStrategyCount();
        if (!rule.check(account.getStrategies().size() + 1)) {
            throw new AddStrategyException("User strategies number under account(" + accountCore.getAccountId() + ") exceeds limit " + rule.get() + ".");
        }
        return computeStrategy(accountCore, strategyRule);
    }

    private void checkStrategyRemovable(StrategyCore strategyCore) throws RemoveStrategyException {
        var incompleted = strategyCore.transactions().values().stream()
                .filter(transaction -> transaction.getState().equals(Transaction.EXECUTING) || transaction.getState().equals(Transaction.PENDING)).collect(Collectors.toSet());
        if (incompleted.size() > 0) {
            var iterator = incompleted.iterator();
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
        checkStrategyRemovable(strategyCore);
        strategyCore.setState(Strategy.REMOVED);
        return accountCore.strategies().remove(strategyId);
    }

}
