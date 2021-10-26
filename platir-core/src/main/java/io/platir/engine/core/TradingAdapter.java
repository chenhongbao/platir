package io.platir.engine.core;

import io.platir.Account;
import io.platir.Contract;
import io.platir.Order;
import io.platir.Strategy;
import io.platir.Transaction;
import io.platir.broker.ExecutionListener;
import io.platir.broker.ExecutionReport;
import io.platir.broker.TradingService;
import io.platir.user.CancelOrderException;
import io.platir.user.NewOrderException;
import io.platir.util.Basics;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.stream.Collectors;

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
        TransactionCore transaction = null;
        try {
            transaction = findTransactionForOrder(executionReport.getOrderId());
            updateExecutionReport(transaction, executionReport);
        } catch (NoSuchOrderException exception) {
            Basics.logger().log(Level.SEVERE, "No order found for execution report. {0}", exception.getMessage());
        } catch (IllegalServiceStateException exception) {
            Basics.logger().log(Level.SEVERE, "Illegal trading service report. {0}", exception.getMessage());
        } catch (IllegalAccountStateException exception) {
            /* Transaction must not be null here. */
            var accountId = transaction.getStrategy().getAccount().getAccountId();
            Basics.logger().log(Level.SEVERE, "Illegal account {0} state. {1}", new Object[]{accountId, exception.getMessage()});
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

    private void updateExecutionReport(TransactionCore transaction, ExecutionReport report) throws NoSuchOrderException, IllegalServiceStateException, IllegalAccountStateException {
        switch (report.getState()) {
            case Order.ALL_TRADED:
                executingTransactions.remove(report.getOrderId());
            case Order.QUEUEING:
                updateContracts(transaction.getStrategy().getAccount(), report);
                updateOrder(findUpdatedOrder(transaction, report), report);
                updateTransaction(transaction);
                break;
            case Order.CANCELED:
            case Order.REJECTED:
                cancelTransaction(transaction, report);
                break;
            default:
                throw new IllegalServiceStateException("Illegal execution state(" + report.getState() + ").");
        }

    }

    private OrderCore findUpdatedOrder(TransactionCore transaction, ExecutionReport report) throws NoSuchOrderException {
        synchronized (transaction) {
            for (var order : transaction.orderMap().values()) {
                if (order.getInstrumentId().equals(report.getInstrumentId()) && order.getExchangeId().equals(report.getExchangeId()) && order.getDirection().equals(report.getDirection()) && order.getOffset().equals(report.getOffset())) {
                    return order;
                }
            }
            throw new NoSuchOrderException("Order(" + report.getInstrumentId() + ", " + report.getExchangeId() + ", " + report.getDirection() + ", " + report.getOffset() + ").");
        }
    }

    private void updateOrder(OrderCore order, ExecutionReport report) throws IllegalServiceStateException {
        synchronized (order) {
            if (report.getTradedQuantity().equals(report.getQuantity())) {
                order.setState(Order.ALL_TRADED);
            } else if (report.getTradedQuantity() > report.getQuantity()) {
                throw new IllegalServiceStateException("Traded quantity(" + report.getTradedQuantity() + ") exceeds quantity(" + report.getQuantity() + ").");
            } else {
                order.setState(Order.QUEUEING);
            }
        }
    }

    private void updateContracts(AccountCore account, ExecutionReport report) throws IllegalAccountStateException {
        synchronized (account) {
            int updatedCount = updateContractStates(findUpdatedContracts(account, report), report.getLastTradedQuantity());
            if (updatedCount < report.getLastTradedQuantity()) {
                throw new IllegalAccountStateException("Need " + report.getLastTradedQuantity() + " contracts to update but got " + updatedCount + ".");
            }
        }
    }

    private Collection<ContractCore> findUpdatedContracts(AccountCore account, ExecutionReport report) {
        return account.contractMap().values().stream()
                .filter(contract -> {
                    return contract.getInstrumentId().equals(report.getInstrumentId()) && contract.getExchangeId().equals(report.getExchangeId());
                })
                .filter(contract -> {
                    if (report.getOffset().equals(Order.OPEN)) {
                        return contract.getDirection().equals(report.getDirection()) && contract.getState().equals(Contract.OPENING);
                    } else {
                        return !contract.getDirection().equals(report.getDirection()) && contract.getState().equals(Contract.CLOSING);
                    }
                }).collect(Collectors.toSet());
    }

    private int updateContractStates(Collection<ContractCore> contracts, int quantity) {
        int updatedCount = 0;
        var iterator = contracts.iterator();
        while (updatedCount < quantity && iterator.hasNext()) {
            var contract = iterator.next();
            if (contract.getState().equals(Contract.OPENING)) {
                contract.setState(Contract.OPEN);
            } else {
                contract.setState(Contract.CLOSED);
            }
            ++updatedCount;
        }
        return updatedCount;
    }

    private void updateTransaction(TransactionCore transaction) throws IllegalAccountStateException {
        int allTradedCount = 0;
        int queueingCount = 0;
        int canceledCount = 0;
        int rejectedCount = 0;
        synchronized (transaction) {
            for (var order : transaction.getOrders()) {
                switch (order.getState()) {
                    case Order.ALL_TRADED:
                        ++allTradedCount;
                        break;
                    case Order.QUEUEING:
                        ++queueingCount;
                        break;
                    case Order.CANCELED:
                        ++canceledCount;
                        break;
                    case Order.REJECTED:
                        ++rejectedCount;
                        break;
                    default:
                        throw new IllegalAccountStateException("Illegal order state(" + order.getState() + ").");
                }
            }
            if (queueingCount > 0) {
                transaction.setState(Transaction.EXECUTING);
            } else if (allTradedCount == 0) {
                transaction.setState(Transaction.REJECTED);
            } else {
                transaction.setState(Transaction.REJECTED);
            }
        }
    }

    private void cancelTransaction(TransactionCore transaction, ExecutionReport report) throws NoSuchOrderException, IllegalAccountStateException {
        var order = findUpdatedOrder(transaction, report);
        synchronized (order) {
            order.setState(report.getState());
        }
        var account = transaction.getStrategy().getAccount();
        synchronized (account) {
            int canceledCount = cancelContractStates(findUpdatedContracts(account, report), report.getQuantity() - report.getTradedQuantity());
            if (canceledCount < report.getLastTradedQuantity()) {
                throw new IllegalAccountStateException("Need " + report.getLastTradedQuantity() + " contracts to cancel but got " + canceledCount + ".");
            }
        }
    }

    private int cancelContractStates(Collection<ContractCore> contracts, int quantity) {
        int canceledCount = 0;
        var iterator = contracts.iterator();
        while (canceledCount < quantity && iterator.hasNext()) {
            var contract = iterator.next();
            if (contract.getState().equals(Contract.OPENING)) {
                contract.setState(Contract.ABANDONED);
            } else {
                contract.setState(Contract.OPEN);
            }
            ++canceledCount;
        }
        return canceledCount;
    }
}
