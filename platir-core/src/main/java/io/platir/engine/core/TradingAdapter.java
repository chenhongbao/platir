package io.platir.engine.core;

import io.platir.Account;
import io.platir.Order;
import io.platir.Strategy;
import io.platir.Transaction;
import io.platir.broker.ExecutionListener;
import io.platir.broker.ExecutionReport;
import io.platir.broker.TradingService;
import io.platir.user.NewOrderException;

class TradingAdapter implements ExecutionListener {

    private final InfoHelper infoHelper;
    private final TradingService tradingService;

    TradingAdapter(TradingService tradingService, InfoHelper infoHelper) {
        this.infoHelper = infoHelper;
        this.tradingService = tradingService;
    }

    @Override
    public void onExecutionReport(ExecutionReport executionReport) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    Transaction newOrderSingle(Strategy strategy, String instrumentId, String exchangeId, Double price, Integer quantity, String direction, String offset) throws NewOrderException {
        synchronized (strategy.getAccount()) {
            TransactionCore transaction;
            switch (offset) {
                case Order.OPEN:
                    transaction = allocateOpenOrderSingle(strategy.getAccount(), instrumentId, exchangeId, price, quantity, direction);
                    break;
                case Order.CLOSE:
                    transaction = allocateAutoCloseOrderSingle(strategy.getAccount(), instrumentId, exchangeId, price, quantity, direction);
                    break;
                case Order.CLOSE_TODAY:
                    transaction = allocateCloseTodayOrderSingle(strategy.getAccount(), instrumentId, exchangeId, price, quantity, direction);
                    break;
                case Order.CLOSE_YESTERDAY:
                    transaction = allocateCloseYesterdayOrderSingle(strategy.getAccount(), instrumentId, exchangeId, price, quantity, direction);
                    break;
                default:
                    throw new NewOrderException("Invalid offset(" + offset + ").");
            }
            executeTransaction(transaction);
            return transaction;
        }
    }

    private TransactionCore allocateOpenOrderSingle(Account account, String instrumentId, String exchangeId, Double price, Integer quantity, String direction) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    private TransactionCore allocateAutoCloseOrderSingle(Account account, String instrumentId, String exchangeId, Double price, Integer quantity, String direction) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    private TransactionCore allocateCloseTodayOrderSingle(Account account, String instrumentId, String exchangeId, Double price, Integer quantity, String direction) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    private TransactionCore allocateCloseYesterdayOrderSingle(Account account, String instrumentId, String exchangeId, Double price, Integer quantity, String direction) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    private void executeTransaction(TransactionCore transaction) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
}
