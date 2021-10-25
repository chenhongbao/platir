package io.platir.core;

import io.platir.Account;
import io.platir.Order;
import io.platir.Transaction;
import io.platir.broker.TradingService;
import io.platir.user.NewOrderException;

class TransactionAdapter {

    private final InfoHelper infoHelper;
    private final TradingService tradingService;
    private final ExecutionUpdater executionUpdater;

    TransactionAdapter(InfoHelper infoHelper, TradingService tradingService, ExecutionUpdater executionUpdater) {
        this.infoHelper = infoHelper;
        this.tradingService = tradingService;
        this.executionUpdater = executionUpdater;
    }

    Transaction newOrderSingle(Account account, String instrumentId, String exchangeId, Double price, Integer quantity, String direction, String offset) throws NewOrderException {
        synchronized (account) {
            TransactionCore transaction;
            switch (offset) {
                case Order.OPEN:
                    transaction = newOpenOrderSingle(account, instrumentId, exchangeId, price, quantity, direction);
                    break;
                case Order.CLOSE:
                    transaction =  newAutoCloseOrderSingle(account, instrumentId, exchangeId, price, quantity, direction);
                    break;
                case Order.CLOSE_TODAY:
                    transaction =  newCloseTodayOrderSingle(account, instrumentId, exchangeId, price, quantity, direction);
                    break;
                case Order.CLOSE_YESTERDAY:
                    transaction =  newCloseYesterdayOrderSingle(account, instrumentId, exchangeId, price, quantity, direction);
                    break;
                default:
                    throw new NewOrderException("Invalid offset(" + offset + ").");
            }
            executionUpdater.registerTransaction(transaction);
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
}
