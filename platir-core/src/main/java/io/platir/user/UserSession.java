package io.platir.core;

import io.platir.Account;
import io.platir.Order;
import io.platir.Strategy;
import io.platir.Transaction;
import io.platir.core.TransactionAdapter;
import io.platir.user.Session;
import java.util.Map;
import java.util.logging.Handler;
import java.util.logging.Logger;

 class UserSession implements Session {

    private final Strategy strategy;
    private final Logger logger;
    private final TransactionAdapter transactionAdapter;

    UserSession(Strategy strategy, TransactionAdapter transactionAdapter, Handler loggingHandler) {
        this.strategy = strategy;
        this.transactionAdapter = transactionAdapter;
        this.logger = Logger.getLogger(strategy.getStrategyId());
        this.logger.addHandler(loggingHandler);
        this.logger.setUseParentHandlers(false);
    }

    @Override
    public Transaction buyOpen(String instrumentId, String exchangeId, Double price, Integer quantity) {
        return transactionAdapter.newOrderSingle(strategy.getAccount(), instrumentId, exchangeId, price, quantity, Order.BUY, Order.OPEN);
    }

    @Override
    public Transaction sellOpen(String instrumentId, String exchangeId, Double price, Integer quantity) {
        return transactionAdapter.newOrderSingle(strategy.getAccount(), instrumentId, exchangeId, price, quantity, Order.SELL, Order.OPEN);
    }

    @Override
    public Transaction buyClose(String instrumentId, String exchangeId, Double price, Integer quantity) {
        return transactionAdapter.newOrderSingle(strategy.getAccount(), instrumentId, exchangeId, price, quantity, Order.BUY, Order.CLOSE);
    }

    @Override
    public Transaction sellClose(String instrumentId, String exchangeId, Double price, Integer quantity) {
        return transactionAdapter.newOrderSingle(strategy.getAccount(), instrumentId, exchangeId, price, quantity, Order.SELL, Order.CLOSE);
    }

    @Override
    public Account getAccount() {
        return strategy.getAccount();
    }

    @Override
    public Logger getLogger() {
        return logger;
    }

    @Override
    public Map<String, String> getParameters() {
        return strategy.getParameters();
    }

}
