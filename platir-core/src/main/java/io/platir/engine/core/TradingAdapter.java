package io.platir.engine.core;

import io.platir.commons.TransactionCore;
import io.platir.commons.OrderCore;
import io.platir.commons.ContractCore;
import io.platir.commons.AccountCore;
import io.platir.commons.StrategyCore;
import io.platir.commons.TradeCore;
import io.platir.Contract;
import io.platir.Order;
import io.platir.Strategy;
import io.platir.Transaction;
import io.platir.broker.ExecutionListener;
import io.platir.broker.ExecutionReport;
import io.platir.broker.TradingService;
import io.platir.user.CancelOrderException;
import io.platir.user.NewOrderException;
import io.platir.utils.Utils;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.stream.Collectors;

class TradingAdapter implements ExecutionListener {

    private final TradingService tradingService;
    private final UserStrategyLookup userStrategyLookup;
    private final AtomicInteger contractIdCounter = new AtomicInteger(0);
    private final AtomicInteger tradeIdCounter = new AtomicInteger(0);
    private final AtomicInteger orderIdCounter = new AtomicInteger(0);
    private final AtomicInteger transactionIdCounter = new AtomicInteger(0);
    private final Map<String /* OrderId */, TransactionCore> executingTransactions = new ConcurrentHashMap<>();
    private final Map<String /* OrderId */, ExecutionReport> lastExecutionReports = new ConcurrentHashMap<>();

    TradingAdapter(TradingService tradingService, UserStrategyLookup userStrategyLookup) {
        this.tradingService = tradingService;
        this.userStrategyLookup = userStrategyLookup;
    }

    boolean isTransactionAllDone() {
        return executingTransactions.isEmpty() && lastExecutionReports.isEmpty();
    }
    
    void forceCancelAll() throws ForceCancelException {
        for (var orderId : executingTransactions.keySet()) {
            forceCancel(orderId);
        }
    }

    void forceCancel(String orderId) throws ForceCancelException {
        var report = lastExecutionReports.remove(orderId);
        var transaction = executingTransactions.remove(orderId);
        if (report == null || transaction == null) {
            throw new ForceCancelException("No execution report or transaction for order(" + orderId + ") force cancel.");
        }
        try {
            cancelOrder(transaction, report, Order.CANCELED);
            cancelTransaction(transaction, report);
        } catch (NoSuchOrderException | IllegalAccountStateException | IllegalServiceStateException exception) {
            throw new ForceCancelException("Force cancel throws exception. " + exception.getMessage());
        }
    }

    @Override
    public void onExecutionReport(ExecutionReport executionReport) {
        /* Save last execution report for settlement canceling. */
        lastExecutionReports.put(executionReport.getOrderId(), executionReport);

        TransactionCore transaction = null;
        try {
            transaction = findTransactionForOrder(executionReport.getOrderId());
            updateExecutionReport(transaction, executionReport);
        } catch (NoSuchOrderException exception) {
            PlatirEngineCore.logger().log(Level.SEVERE, "No order found for execution report. {0}", exception.getMessage());
        } catch (IllegalServiceStateException exception) {
            PlatirEngineCore.logger().log(Level.SEVERE, "Illegal trading service report. {0}", exception.getMessage());
        } catch (IllegalAccountStateException exception) {
            var accountId = transaction.getStrategy().getAccount().getAccountId();
            PlatirEngineCore.logger().log(Level.SEVERE, "Illegal account {0} state. {1}", new Object[]{accountId, exception.getMessage()});
        }
    }

    Transaction newOrderSingle(Strategy strategy, String instrumentId,
            String exchangeId, Double price,
            Integer quantity, String direction,
            String offset) throws NewOrderException {
        var strategyCore = (StrategyCore) strategy;
        synchronized (strategyCore.syncObject()) {
            if (!strategy.getState().equals(Strategy.NORMAL)) {
                throw new NewOrderException("Strategy(" + strategy.getStrategyId() + ") is " + strategy.getState() + ".");
            }
        }
        return executeNewOrderSingle(strategyCore, instrumentId, exchangeId, price, quantity, direction, offset);
    }

    private TransactionCore executeNewOrderSingle(StrategyCore strategy, String instrumentId, String exchangeId, Double price, Integer quantity, String direction, String offset) throws NewOrderException {
        TransactionCore transaction;
        switch (offset) {
            case Order.OPEN:
                transaction = allocateOpenOrderSingle(strategy, instrumentId, exchangeId, price, quantity, direction);
                break;
            case Order.CLOSE_TODAY:
                transaction = allocateCloseTodayOrderSingle(strategy, instrumentId, exchangeId, price, quantity, direction);
                break;
            case Order.CLOSE_YESTERDAY:
                transaction = allocateCloseYesterdayOrderSingle(strategy, instrumentId, exchangeId, price, quantity, direction);
                break;
            default:
                throw new NewOrderException("Invalid offset(" + offset + ").");
        }
        executeTransaction(transaction);
        return transaction;
    }

    void cancelOrderSingle(TransactionCore transaction) throws CancelOrderException {
        Set<String> failed = new HashSet<>();
        transaction.getOrders().forEach(order -> {
            int code = tradingService.orderCancelRequest(order);
            if (code != 0) {
                failed.add(order.getOrderId() + "/" + code);
            }
        });
        if (!failed.isEmpty()) {
            var iterator = failed.iterator();
            String message = iterator.next();
            while (iterator.hasNext()) {
                message += ", " + iterator.next();
            }
            throw new CancelOrderException("Failed cancel order(" + message + ").");
        }
    }

    private void setOpeningContracts(AccountCore account, String instrumentId, String exchangeId, Integer quantity, String direction) {
        int count = 0;
        while (count++ < quantity) {
            var contract = new ContractCore();
            contract.setAccountId(account.getAccountId());
            contract.setContractId(account.getAccountId() + "-" + contractIdCounter.incrementAndGet());
            contract.setDirection(direction);
            contract.setExchangeId(exchangeId);
            contract.setInstrumentId(instrumentId);
            contract.setState(Contract.OPENING);
            account.contracts().put(contract.getContractId(), contract);
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
        transaction.setTransactionId(strategy.getStrategyId() + "-" + transactionIdCounter.incrementAndGet());
        transaction.setUpdateDatetime(Utils.datetime());
        for (var order : orders) {
            order.setOrderId(transaction.getTransactionId() + "-" + orderIdCounter.incrementAndGet());
            order.setTransaction(transaction);
            transaction.orders().put(order.getOrderId(), order);
        }
        strategy.transactions().put(transaction.getTransactionId(), transaction);
        return transaction;
    }

    private TransactionCore allocateOpenOrderSingle(StrategyCore strategy, String instrumentId, String exchangeId, Double price, Integer quantity, String direction) throws NewOrderException {
        try {
            var account = strategy.getAccount();
            synchronized (account.syncObject()) {
                var instrument = InfoCenter.getInstrument(instrumentId);
                var needMoney = AccountUtils.computeCommission(instrument, price, quantity) + AccountUtils.computeMargin(instrument, price, quantity);
                AccountUtils.settleAccount(account, AccountUtils.findInstruments(account), AccountUtils.findLatestPrices(account), InfoCenter.getTradingDay());
                if (account.getAvailable() < needMoney) {
                    throw new NewOrderException("Insufficient money need " + needMoney + " but have " + account.getAvailable() + ".");
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
        return account.contracts().values().stream()
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

    private void setClosingContracts(Set<ContractCore> contracts, Double price, Integer quantity) throws IllegalAccountStateException {
        var count = 0;
        var iterator = contracts.iterator();
        while (count++ < quantity && iterator.hasNext()) {
            var contract = iterator.next();
            contract.setState(Contract.CLOSING);
            contract.setClosePrice(price);
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
            setClosingContracts(contracts, price, quantity);
            return computeTransaction(strategy, instrumentId, exchangeId, price, quantity, direction, Order.CLOSE_TODAY, computeOrder(instrumentId, exchangeId, price, quantity, direction, Order.CLOSE_TODAY));
        } catch (IllegalAccountStateException exception) {
            throw new NewOrderException("Account data illegal for close. " + exception.getMessage(), exception);
        }
    }

    private TransactionCore allocateCloseTodayOrderSingle(StrategyCore strategy, String instrumentId, String exchangeId, Double price, Integer quantity, String direction) throws NewOrderException {
        try {
            var account = strategy.getAccount();
            synchronized (account.syncObject()) {
                Set<ContractCore> contracts = findCloseTodayContracts(account, instrumentId, exchangeId, direction, InfoCenter.getTradingDay());
                return allocateCloseOrderSingle(contracts, strategy, instrumentId, exchangeId, price, quantity, direction);
            }
        } catch (InsufficientInfoException exception) {
            throw new NewOrderException("Insufficient information for close. " + exception.getMessage(), exception);
        }
    }

    private TransactionCore allocateCloseYesterdayOrderSingle(StrategyCore strategy, String instrumentId, String exchangeId, Double price, Integer quantity, String direction) throws NewOrderException {
        try {
            var account = strategy.getAccount();
            synchronized (account.syncObject()) {
                Set<ContractCore> contracts = findCloseYesterdayContracts(account, instrumentId, exchangeId, direction, InfoCenter.getTradingDay());
                return allocateCloseOrderSingle(contracts, strategy, instrumentId, exchangeId, price, quantity, direction);
            }
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
                        PlatirEngineCore.logger().log(Level.SEVERE, "Cancel request returns {0}.", code);
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
        var accountCore = transaction.getStrategy().getAccount();
        switch (report.getState()) {
            case Order.QUEUEING:
                synchronized (accountCore.syncObject()) {
                    updateContracts(transaction.getStrategy().getAccount(), report);
                    updateOrderState(findUpdatedOrder(transaction, report), report);
                    updateTransactionState(transaction);
                }
                break;
            case Order.CANCELED:
            case Order.REJECTED:
                synchronized (accountCore.syncObject()) {
                    cancelOrder(transaction, report, null);
                    cancelTransaction(transaction, report);
                }
            case Order.ALL_TRADED:
                removeLocalRecords(report.getOrderId());
                break;
            default:
                throw new IllegalServiceStateException("Illegal execution state(" + report.getState() + ").");
        }

    }

    private void removeLocalRecords(String orderId) {
        lastExecutionReports.remove(orderId);
        executingTransactions.remove(orderId);
    }

    private OrderCore findUpdatedOrder(TransactionCore transaction, ExecutionReport report) throws NoSuchOrderException {
        for (var order : transaction.orders().values()) {
            if (order.getInstrumentId().equals(report.getInstrumentId()) && order.getExchangeId().equals(report.getExchangeId()) && order.getDirection().equals(report.getDirection()) && order.getOffset().equals(report.getOffset())) {
                return order;
            }
        }
        throw new NoSuchOrderException("Order(" + report.getInstrumentId() + ", " + report.getExchangeId() + ", " + report.getDirection() + ", " + report.getOffset() + ").");
    }

    private TradeCore computeTrade(OrderCore order, ExecutionReport report) {
        /* Sometimes execution report doesn't provide some fields. They are taken from order. */
        var trade = new TradeCore();
        trade.setTradeId(order.getOrderId() + "-" + tradeIdCounter.incrementAndGet());
        trade.setDirection(order.getDirection());
        trade.setInstrumentId(order.getInstrumentId());
        trade.setExchangeId(order.getExchangeId());
        trade.setOffset(order.getOffset());
        trade.setOrder(order);
        trade.setPrice(report.getLastTradedPirce());
        trade.setQuantity(report.getLastTradedQuantity());
        trade.setTradingDay(report.getTradingDay());
        trade.setUpdateDatetime(report.getUpdateTime());
        return trade;
    }

    private void updateOrderState(OrderCore order, ExecutionReport report) throws IllegalServiceStateException {
        TradeCore trade = computeTrade(order, report);
        order.trades().put(trade.getTradeId(), trade);
        if (report.getTradedQuantity().equals(report.getQuantity())) {
            order.setState(Order.ALL_TRADED);
        } else if (report.getTradedQuantity() > report.getQuantity()) {
            throw new IllegalServiceStateException("Traded quantity(" + report.getTradedQuantity() + ") exceeds quantity(" + report.getQuantity() + ").");
        } else {
            order.setState(Order.QUEUEING);
        }
    }

    private void updateContracts(AccountCore account, ExecutionReport report) throws IllegalAccountStateException, IllegalServiceStateException {
        int updatedCount = updateTradedContracts(findUpdatedContracts(account, report), report);
        if (updatedCount < report.getLastTradedQuantity()) {
            throw new IllegalAccountStateException("Need " + report.getLastTradedQuantity() + " contracts to update but got " + updatedCount + ".");
        }
    }

    private Collection<ContractCore> findUpdatedContracts(AccountCore account, ExecutionReport report) throws IllegalServiceStateException {
        try {
            return account.contracts().values().stream()
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
                contract.setOpenDatetime(Utils.datetime());
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

        for (var order : transaction.orders().values()) {
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
        transaction.setUpdateDatetime(Utils.datetime());
        /*
         * Execution update changes the account state, so the following update
         * could change the account before preceeding call returns, leading to
         * account state inconsistence. Here invokes user callback as soon as 
         * update arrives and the synchronization is left to user.
         */
        PlatirEngineCore.threads().submit(() -> {
            StrategyCore strategy = null;
            try {
                strategy = transaction.getStrategy();
                userStrategyLookup.findStrategy(strategy).onTransaction(transaction);
            } catch (Throwable throwable) {
                PlatirEngineCore.logger().log(Level.SEVERE, "Strategy({0}) callback throws exception. {1}", new Object[]{strategy.getStrategyId(), throwable.getMessage()});
            }
        });
    }

    private void cancelOrder(TransactionCore transaction, ExecutionReport report, String state) throws NoSuchOrderException {
        findUpdatedOrder(transaction, report).setState(state == null ? report.getState() : state);
    }

    private void cancelTransaction(TransactionCore transaction, ExecutionReport report) throws IllegalAccountStateException, IllegalServiceStateException {
        var account = transaction.getStrategy().getAccount();
        var needCancel = report.getQuantity() - report.getTradedQuantity();
        int canceledCount = cancelContractStates(findUpdatedContracts(account, report), needCancel);
        if (canceledCount < needCancel) {
            throw new IllegalAccountStateException("Need " + needCancel + " contracts to cancel but got " + canceledCount + ".");
        }
        updateTransactionState(transaction);
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
