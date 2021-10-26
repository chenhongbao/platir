package io.platir.engine.core;

import io.platir.Account;
import io.platir.Order;
import io.platir.Transaction;
import io.platir.user.MarketDataRequestException;
import io.platir.user.NewOrderException;
import io.platir.user.Session;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Handler;
import java.util.logging.Logger;

class UserSession implements Session {

    private final StrategyCore strategy;
    private final Logger logger;
    private final TransactionAdapter transactionAdapter;
    private final MarketDataAdapter marketDataAdapter;

    UserSession(StrategyCore strategy, TransactionAdapter transactionAdapter, MarketDataAdapter marketDataAdapter, Handler loggingHandler) {
        this.strategy = strategy;
        this.transactionAdapter = transactionAdapter;
        this.marketDataAdapter = marketDataAdapter;
        this.logger = Logger.getLogger(strategy.getStrategyId());
        this.logger.addHandler(loggingHandler);
        this.logger.setUseParentHandlers(false);
    }

    @Override
    public Transaction buyOpen(String instrumentId, String exchangeId, Double price, Integer quantity) throws NewOrderException {
        return transactionAdapter.newOrderSingle(strategy, instrumentId, exchangeId, price, quantity, Order.BUY, Order.OPEN);
    }

    @Override
    public Transaction sellOpen(String instrumentId, String exchangeId, Double price, Integer quantity) throws NewOrderException {
        return transactionAdapter.newOrderSingle(strategy, instrumentId, exchangeId, price, quantity, Order.SELL, Order.OPEN);
    }

    @Override
    public Transaction buyClose(String instrumentId, String exchangeId, Double price, Integer quantity) throws NewOrderException {
        return transactionAdapter.newOrderSingle(strategy, instrumentId, exchangeId, price, quantity, Order.BUY, Order.CLOSE);
    }

    @Override
    public Transaction sellClose(String instrumentId, String exchangeId, Double price, Integer quantity) throws NewOrderException {
        return transactionAdapter.newOrderSingle(strategy, instrumentId, exchangeId, price, quantity, Order.SELL, Order.CLOSE);
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
        return new HashMap<>(strategy.getStrategyRule().parameters());
    }

    @Override
    public String marketDataRequest(String instrumentId) throws MarketDataRequestException {
        return marketDataAdapter.marketDataRequest(strategy, instrumentId);
    }

}
