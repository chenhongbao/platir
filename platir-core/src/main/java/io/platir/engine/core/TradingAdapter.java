package io.platir.engine.core;

import io.platir.Account;
import io.platir.Order;
import io.platir.Strategy;
import io.platir.Transaction;
import io.platir.broker.ExecutionListener;
import io.platir.broker.ExecutionReport;
import io.platir.broker.TradingService;
import io.platir.user.CancelOrderException;
import io.platir.user.NewOrderException;
import io.platir.util.Basics;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

class TradingAdapter implements ExecutionListener {

    private final InfoHelper infoHelper;
    private final TradingService tradingService;
    private final Map<String /* OrderId */, TransactionCore> executingTransactions = new ConcurrentHashMap<>();

    TradingAdapter(TradingService tradingService, InfoHelper infoHelper) {
        this.infoHelper = infoHelper;
        this.tradingService = tradingService;
    }

    @Override
    public void onExecutionReport(ExecutionReport executionReport) {
        try {
            updateExecutionReport(findTransactionForOrder(executionReport.getOrderId()), executionReport);
        } catch (NoSuchOrderException exception) {
            Basics.logger().log(Level.SEVERE, "No order found for execution report. {0}", exception.getMessage());
        }
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
    
    void cancelOrderSingle(TransactionCore transaction) throws CancelOrderException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
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

    private TransactionCore findTransactionForOrder(String orderId) throws NoSuchOrderException {
        var transaction = executingTransactions.get(orderId);
        if (transaction == null) {
            throw new NoSuchOrderException("Order(" + orderId + ").");
        }
        return transaction;
    }

    private void updateExecutionReport(TransactionCore transaction, ExecutionReport report) throws NoSuchOrderException {
        updateContracts(transaction.getStrategy().getAccount(), report);
        updateOrder(findUpdatedOrder(transaction, report), report);
        updateTransaction(transaction);
    }

    private OrderCore findUpdatedOrder(TransactionCore transaction, ExecutionReport report) throws NoSuchOrderException {
        for (var order : transaction.orderMap().values()) {
            if (order.getInstrumentId().equals(report.getInstrumentId()) && order.getExchangeId().equals(report.getExchangeId()) && order.getDirection().equals(report.getDirection()) && order.getOffset().equals(report.getOffset())) {
                return order;
            }
        }
        throw new NoSuchOrderException("Order(" + report.getInstrumentId() + ", " + report.getExchangeId() + ", " + report.getDirection() + ", " + report.getOffset() + ").");
    }

    private void updateOrder(OrderCore order, ExecutionReport report) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    private void updateContracts(AccountCore account, ExecutionReport report) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    private void updateTransaction(TransactionCore transaction) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
}
