package io.platir.engine.core;

import io.platir.Account;
import io.platir.Instrument;
import io.platir.Order;
import io.platir.Transaction;
import io.platir.user.CancelOrderException;
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
    private final TradingAdapter tradingAdapter;
    private final MarketDataAdapter marketDataAdapter;

    UserSession(StrategyCore strategy, TradingAdapter transactionAdapter, MarketDataAdapter marketDataAdapter, Handler loggingHandler) {
        this.strategy = strategy;
        this.tradingAdapter = transactionAdapter;
        this.marketDataAdapter = marketDataAdapter;
        this.logger = Logger.getLogger(strategy.getStrategyId());
        this.logger.addHandler(loggingHandler);
        this.logger.setUseParentHandlers(false);
    }

    @Override
    public Transaction buyOpen(String instrumentId, String exchangeId, Double price, Integer quantity) throws NewOrderException {
        return tradingAdapter.newOrderSingle(strategy, instrumentId, exchangeId, price, quantity, Order.BUY, Order.OPEN);
    }

    @Override
    public Transaction sellOpen(String instrumentId, String exchangeId, Double price, Integer quantity) throws NewOrderException {
        return tradingAdapter.newOrderSingle(strategy, instrumentId, exchangeId, price, quantity, Order.SELL, Order.OPEN);
    }

    @Override
    public Transaction buyCloseToday(String instrumentId, String exchangeId, Double price, Integer quantity) throws NewOrderException {
        return tradingAdapter.newOrderSingle(strategy, instrumentId, exchangeId, price, quantity, Order.BUY, Order.CLOSE_TODAY);
    }

    @Override
    public Transaction buyCloseYesterday(String instrumentId, String exchangeId, Double price, Integer quantity) throws NewOrderException {
        return tradingAdapter.newOrderSingle(strategy, instrumentId, exchangeId, price, quantity, Order.BUY, Order.CLOSE_YESTERDAY);
    }

    @Override
    public Transaction sellCloseToday(String instrumentId, String exchangeId, Double price, Integer quantity) throws NewOrderException {
        return tradingAdapter.newOrderSingle(strategy, instrumentId, exchangeId, price, quantity, Order.SELL, Order.CLOSE_TODAY);
    }

    @Override
    public Transaction sellCloseYesterday(String instrumentId, String exchangeId, Double price, Integer quantity) throws NewOrderException {
        return tradingAdapter.newOrderSingle(strategy, instrumentId, exchangeId, price, quantity, Order.SELL, Order.CLOSE_YESTERDAY);
    }

    @Override
    public void cancel(Transaction transaction) throws CancelOrderException {
        tradingAdapter.cancelOrderSingle((TransactionCore) transaction);
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
    public void marketDataRequest(String instrumentId) throws MarketDataRequestException {
        marketDataAdapter.marketDataRequest(strategy, instrumentId);
    }

    @Override
    public Instrument getInstrument(String instrumentId) {
        try {
            return InfoCenter.getInstrument(instrumentId);
        } catch (InsufficientInfoException ex) {
            return null;
        }
    }

}
