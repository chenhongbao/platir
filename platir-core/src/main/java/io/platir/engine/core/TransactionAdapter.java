package io.platir.engine.core;

import io.platir.Account;
import io.platir.Order;
import io.platir.Strategy;
import io.platir.Transaction;
import io.platir.broker.ExecutionListener;
import io.platir.broker.ExecutionReport;
import io.platir.broker.TradingService;
import io.platir.user.NewOrderException;

class TransactionAdapter implements ExecutionListener {

    private final InfoHelper infoHelper;
    private final TradingService tradingService;

    TransactionAdapter(InfoHelper infoHelper, TradingService tradingService) {
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
                    transaction = newOpenOrderSingle(strategy.getAccount(), instrumentId, exchangeId, price, quantity, direction);
                    break;
                case Order.CLOSE:
                    transaction = newAutoCloseOrderSingle(strategy.getAccount(), instrumentId, exchangeId, price, quantity, direction);
                    break;
                case Order.CLOSE_TODAY:
                    transaction = newCloseTodayOrderSingle(strategy.getAccount(), instrumentId, exchangeId, price, quantity, direction);
                    break;
                case Order.CLOSE_YESTERDAY:
                    transaction = newCloseYesterdayOrderSingle(strategy.getAccount(), instrumentId, exchangeId, price, quantity, direction);
                    break;
                default:
                    throw new NewOrderException("Invalid offset(" + offset + ").");
            }
            registerTransaction(transaction);
            return transaction;
        }
    }

    private TransactionCore newOpenOrderSingle(Account account, String instrumentId, String exchangeId, Double price, Integer quantity, String direction) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    private TransactionCore newAutoCloseOrderSingle(Account account, String instrumentId, String exchangeId, Double price, Integer quantity, String direction) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    private TransactionCore newCloseTodayOrderSingle(Account account, String instrumentId, String exchangeId, Double price, Integer quantity, String direction) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    private TransactionCore newCloseYesterdayOrderSingle(Account account, String instrumentId, String exchangeId, Double price, Integer quantity, String direction) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    private void registerTransaction(TransactionCore transaction) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
}
