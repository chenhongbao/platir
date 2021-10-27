package io.platir.engine.core;

import io.platir.Account;
import io.platir.Strategy;
import io.platir.User;
import io.platir.engine.rule.AccountRule;
import io.platir.engine.rule.StrategyRule;
import io.platir.engine.rule.UserRule;
import java.util.Set;

class UserManager {

    Set<User> getUsers() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    User addUser(String userId, String password, UserRule userRule) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    void removeUser(String userId) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    Account addAccount(Double initialBalance, User user, AccountRule accountRule) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    void removeAccount(String accountId, User user) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    Strategy addStrategy(Account account, StrategyRule strategyRule) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    Strategy removeStrategy(String strategyId) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
}
