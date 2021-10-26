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
import io.platir.util.Utils;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.stream.Collectors;

class TradingAdapter implements ExecutionListener {

    private final InfoHelper infoHelper;
    private final TradingService tradingService;
    private final AtomicInteger contractIdCounter = new AtomicInteger(0);
    private final AtomicInteger orderIdCounter = new AtomicInteger(0);
    private final AtomicInteger transactionIdCounter = new AtomicInteger(0);
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
            Utils.logger().log(Level.SEVERE, "No order found for execution report. {0}", exception.getMessage());
        } catch (IllegalServiceStateException exception) {
            Utils.logger().log(Level.SEVERE, "Illegal trading service report. {0}", exception.getMessage());
        } catch (IllegalAccountStateException exception) {
            /* Transaction must not be null here. */
            var accountId = transaction.getStrategy().getAccount().getAccountId();
            Utils.logger().log(Level.SEVERE, "Illegal account {0} state. {1}", new Object[]{accountId, exception.getMessage()});
        }
    }

    Transaction newOrderSingle(Strategy strategy, String instrumentId, String exchangeId, Double price, Integer quantity, String direction, String offset) throws NewOrderException {
        TransactionCore transaction;
        switch (offset) {
            case Order.OPEN:
                transaction = allocateOpenOrderSingle((StrategyCore) strategy, instrumentId, exchangeId, price, quantity, direction);
                break;
            case Order.CLOSE_TODAY:
                transaction = allocateCloseTodayOrderSingle((StrategyCore) strategy, instrumentId, exchangeId, price, quantity, direction);
                break;
            case Order.CLOSE_YESTERDAY:
                transaction = allocateCloseYesterdayOrderSingle((StrategyCore) strategy, instrumentId, exchangeId, price, quantity, direction);
                break;
            default:
                throw new NewOrderException("Invalid offset(" + offset + ").");
        }
        executeTransaction(transaction);
        return transaction;
    }

    void cancelOrderSingle(TransactionCore transaction) throws CancelOrderException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    private Map<String, Double> findLatestPrices(Account account) {
        final Map<String, Double> prices = new HashMap<>();
        account.getContracts().stream()
                .map(contract -> contract.getInstrumentId())
                .collect(Collectors.toSet())
                .forEach(instrumentId -> {
                    try {
                        prices.put(instrumentId, infoHelper.getLatestPrice(instrumentId));
                    } catch (InsufficientInfoException ignored) {
                    }
                });
        return prices;
    }

    private void setOpeningContracts(AccountCore account, String instrumentId, String exchangeId, Integer quantity, String direction) {
        int count = 0;
        while (count++ < quantity) {
            var contract = new ContractCore();
            contract.setAccountId(account.getAccountId());
            contract.setContractId(account.getAccountId() + "." + contractIdCounter.incrementAndGet());
            contract.setDirection(direction);
            contract.setExchangeId(exchangeId);
            contract.setInstrumentId(instrumentId);
            contract.setState(Contract.OPENING);
            account.contractMap().put(contract.getContractId(), contract);
        }
    }

    private OrderCore computeOrder(String instrumentId, String exchangeId, Double price, Integer quantity, String direction, String offset) {
        var order = new OrderCore();
        order.setDirection(direction);
        order.setExchangeId(exchangeId);
        order.setInstrumentId(instrumentId);
        order.setOffset(offset);
        order.setPrice(price);
        order.setQuantity(quantity);
        return order;
    }

    private TransactionCore computeTransaction(StrategyCore strategy, String instrumentId, String exchangeId, Double price, Integer quantity, String direction, String offset, OrderCore... orders) {
        var transaction = new TransactionCore();
        transaction.setDirection(direction);
        transaction.setExchangeId(exchangeId);
        transaction.setInstrumentId(instrumentId);
        transaction.setOffset(offset);
        transaction.setPrice(price);
        transaction.setQuantity(quantity);
        transaction.setState(Transaction.PENDING);
        transaction.setStrategy(strategy);
        transaction.setTransactionId(strategy.getStrategyId() + "." + transactionIdCounter.incrementAndGet());
        transaction.setUpdateDateTime(Utils.datetime());
        for (var order : orders) {
            order.setOrderId(transaction.getTransactionId() + "." + orderIdCounter.incrementAndGet());
            order.setTransaction(transaction);
            transaction.orderMap().put(order.getOrderId(), order);
        }
        return transaction;
    }

    private TransactionCore allocateOpenOrderSingle(StrategyCore strategy, String instrumentId, String exchangeId, Double price, Integer quantity, String direction) throws NewOrderException {
        try {
            synchronized (strategy.getAccount()) {
                var instrument = infoHelper.getInstrument(instrumentId);
                var needMoney = AccountUtils.computeCommission(instrument, price, quantity) + AccountUtils.computeMargin(instrument, price, quantity);
                var available = AccountUtils.computeAvailable(strategy.getAccount(), findLatestPrices(strategy.getAccount()), infoHelper.getTradingDay());
                if (available < needMoney) {
                    throw new NewOrderException("Insufficient money need " + needMoney + " but have " + available + ".");
                }
                setOpeningContracts(strategy.getAccount(), instrumentId, exchangeId, quantity, direction);
                return computeTransaction(strategy, instrumentId, exchangeId, price, quantity, direction, Order.OPEN, computeOrder(instrumentId, exchangeId, price, quantity, direction, Order.OPEN));
            }
        } catch (InsufficientInfoException exception) {
            throw new NewOrderException("Insufficient information for new order. " + exception.getMessage(), exception);
        }
    }

    private Set<ContractCore> findCloseContracts(AccountCore account, String instrumentId, String exchangeId, String direction) {
        var targetDirection = direction.equals(Order.BUY) ? Order.SELL : Order.BUY;
        return account.contractMap().values().stream()
                .filter(contract -> contract.getInstrumentId().equals(instrumentId)
                && contract.getExchangeId().equals(exchangeId)
                && contract.getDirection().equals(targetDirection)
                && contract.getState().equals(Contract.OPEN))
                .collect(Collectors.toSet());
    }

    private Set<ContractCore> findCloseTodayContracts(AccountCore account, String instrumentId, String exchangeId, String direction, String tradingDay) {
        return findCloseContracts(account, instrumentId, exchangeId, direction).stream()
                .filter(contract -> contract.getOpenTradingDay().equals(tradingDay))
                .collect(Collectors.toSet());
    }

    private Set<ContractCore> findCloseYesterdayContracts(AccountCore account, String instrumentId, String exchangeId, String direction, String tradingDay) {
        return findCloseContracts(account, instrumentId, exchangeId, direction).stream()
                .filter(contract -> !contract.getOpenTradingDay().equals(tradingDay))
                .collect(Collectors.toSet());
    }

    private void setClosingContracts(Set<ContractCore> contracts, Integer quantity) throws IllegalAccountStateException {
        var count = 0;
        var iterator = contracts.iterator();
        while (count++ < quantity && iterator.hasNext()) {
            var contract = iterator.next();
            contract.setState(Contract.CLOSING);
        }
        if (count < quantity) {
            throw new IllegalAccountStateException("Need " + quantity + " contracts to close but have " + count + ".");
        }
    }

    private TransactionCore allocateCloseOrderSingle(Set<ContractCore> contracts, StrategyCore strategy, String instrumentId, String exchangeId, Double price, Integer quantity, String direction) throws NewOrderException {
        try {
            if (contracts.size() < quantity) {
                throw new NewOrderException("Insufficient position need " + quantity + " but have " + contracts.size() + ".");
            }
            setClosingContracts(contracts, quantity);
            return computeTransaction(strategy, instrumentId, exchangeId, price, quantity, direction, Order.CLOSE_TODAY, computeOrder(instrumentId, exchangeId, price, quantity, direction, Order.CLOSE_TODAY));
        } catch (IllegalAccountStateException exception) {
            throw new NewOrderException("Account data illegal for close. " + exception.getMessage(), exception);
        }
    }

    private TransactionCore allocateCloseTodayOrderSingle(StrategyCore strategy, String instrumentId, String exchangeId, Double price, Integer quantity, String direction) throws NewOrderException {
        try {
            Set<ContractCore> contracts = findCloseTodayContracts(strategy.getAccount(), instrumentId, exchangeId, direction, infoHelper.getTradingDay());
            return allocateCloseOrderSingle(contracts, strategy, instrumentId, exchangeId, price, quantity, direction);
        } catch (InsufficientInfoException exception) {
            throw new NewOrderException("Insufficient information for close. " + exception.getMessage(), exception);
        }
    }

    private TransactionCore allocateCloseYesterdayOrderSingle(StrategyCore strategy, String instrumentId, String exchangeId, Double price, Integer quantity, String direction) throws NewOrderException {
        try {
            Set<ContractCore> contracts = findCloseYesterdayContracts(strategy.getAccount(), instrumentId, exchangeId, direction, infoHelper.getTradingDay());
            return allocateCloseOrderSingle(contracts, strategy, instrumentId, exchangeId, price, quantity, direction);
        } catch (InsufficientInfoException exception) {
            throw new NewOrderException("Insufficient information for close. " + exception.getMessage(), exception);
        }
    }

    private void executeTransaction(TransactionCore transaction) throws NewOrderException {
        int returnCode = 0;
        Set<Order> sentOrders = new HashSet<>();
        for (var order : transaction.getOrders()) {
            returnCode = tradingService.newOrderSingle(order, this);
            if (returnCode == 0) {
                sentOrders.add(order);
            } else {
                var iterator = sentOrders.iterator();
                while (iterator.hasNext()) {
                    var sentOrder = iterator.next();
                    int code = tradingService.orderCancelRequest(sentOrder);
                    if (code != 0) {
                        /* Ignore the cancel request failure because this situation needs manual intervention. */
                        Utils.logger().log(Level.SEVERE, "Cancel request returns {0}.", code);
                    }
                    iterator.remove();
                }
                break;
            }
        }
        if (sentOrders.isEmpty()) {
            throw new NewOrderException("New order request returns " + returnCode + ".");
        } else {
            sentOrders.forEach(order -> executingTransactions.put(order.getOrderId(), transaction));
        }
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
                updateOrderState(findUpdatedOrder(transaction, report), report);
                updateTransactionState(transaction);
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

    private void updateOrderState(OrderCore order, ExecutionReport report) throws IllegalServiceStateException {
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

    private void updateContracts(AccountCore account, ExecutionReport report) throws IllegalAccountStateException, IllegalServiceStateException {
        synchronized (account) {
            int updatedCount = updateTradedContracts(findUpdatedContracts(account, report), report);
            if (updatedCount < report.getLastTradedQuantity()) {
                throw new IllegalAccountStateException("Need " + report.getLastTradedQuantity() + " contracts to update but got " + updatedCount + ".");
            }
        }
    }

    private Collection<ContractCore> findUpdatedContracts(AccountCore account, ExecutionReport report) throws IllegalServiceStateException {
        try {
            return account.contractMap().values().stream()
                    .filter(contract -> {
                        return contract.getInstrumentId().equals(report.getInstrumentId()) && contract.getExchangeId().equals(report.getExchangeId());
                    })
                    .filter(contract -> {
                        switch (report.getOffset()) {
                            case Order.OPEN:
                                return contract.getDirection().equals(report.getDirection()) && contract.getState().equals(Contract.OPENING);
                            case Order.CLOSE_TODAY:
                            case Order.CLOSE_YESTERDAY:
                                return !contract.getDirection().equals(report.getDirection()) && contract.getState().equals(Contract.CLOSING);
                            default:
                                throw new RuntimeException("Invalid execution report offset(" + report.getOffset() + ").");
                        }
                    }).collect(Collectors.toSet());
        } catch (RuntimeException exception) {
            throw new IllegalServiceStateException(exception.getMessage(), exception);
        }
    }

    private int updateTradedContracts(Collection<ContractCore> contracts, ExecutionReport report) {
        int updatedCount = 0;
        var iterator = contracts.iterator();
        while (updatedCount < report.getLastTradedQuantity() && iterator.hasNext()) {
            var contract = iterator.next();
            if (contract.getState().equals(Contract.OPENING)) {
                contract.setState(Contract.OPEN);
                contract.setPrice(report.getLastTradedPirce());
                contract.setOpenTradingDay(report.getTradingDay());
                contract.setOpenTime(Utils.datetime());
            } else {
                contract.setState(Contract.CLOSED);
                contract.setClosePrice(report.getLastTradedPirce());
            }
            ++updatedCount;
        }
        return updatedCount;
    }

    private void updateTransactionState(TransactionCore transaction) throws IllegalAccountStateException {
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

    private void cancelTransaction(TransactionCore transaction, ExecutionReport report) throws NoSuchOrderException, IllegalAccountStateException, IllegalServiceStateException {
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
        synchronized (transaction) {
            updateTransactionState(transaction);
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
