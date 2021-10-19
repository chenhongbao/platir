/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package io.platir.core.internals;

import io.platir.service.Account;
import io.platir.service.Contract;
import io.platir.service.User;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 *
 * @author Chen Hongbao
 */
class UserSnapshot {

    private User user;
    private Account account;
    private final Map<String, Set<Contract>> contracts = new HashMap<>();

    User getUser() {
        return user;
    }

    void setUser(User user) {
        this.user = user;
    }

    Account getAccount() {
        return account;
    }

    void setAccount(Account account) {
        this.account = account;
    }

    Map<String, Set<Contract>> contracts() {
        return contracts;
    }

}
