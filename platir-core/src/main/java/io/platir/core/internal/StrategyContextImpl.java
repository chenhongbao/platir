package io.platir.core.internal;

import io.platir.queries.Utils;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.atomic.AtomicBoolean;

import io.platir.core.AnnotationParsingException;
import io.platir.core.IntegrityException;
import io.platir.core.StrategyRemovalException;
import io.platir.service.Bar;
import io.platir.service.InterruptionException;
import io.platir.service.Order;
import io.platir.service.OrderContext;
import io.platir.service.PlatirClient;
import io.platir.service.StrategyContext;
import io.platir.service.StrategyProfile;
import io.platir.service.Tick;
import io.platir.service.Trade;
import io.platir.service.Transaction;
import io.platir.service.TransactionContext;
import io.platir.service.Queries;
import io.platir.service.ServiceConstants;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.logging.FileHandler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import io.platir.service.TradeUpdate;

/**
 *
 * @author Chen Hongbao
 * @since 1.0.0
 */
class StrategyContextImpl implements StrategyContext {

    private final Logger logger;
    private final StrategyProfile strategyProfile;
    private final CompositeStrategy annotatedStrategy;
    private final PlatirClientImpl platirClient;
    private final Set<TransactionContextImpl> transactions = new ConcurrentSkipListSet<>();
    private final StrategyLoggingHandler loggingHandler;
    /*
     * If the strategy is shutdown, no more market data input, but trade response
     * still comes in.
     */
    private final AtomicBoolean isShutdown = new AtomicBoolean(true);
    private final StrategyCallbackQueue callbackQueue;

    StrategyContextImpl(StrategyProfile strategyProfile, Object strategyObject, TransactionQueue transactionQueue, MarketRouter marketQueue, Queries queries) throws AnnotationParsingException {
        this.strategyProfile = strategyProfile;
        this.annotatedStrategy = new CompositeStrategy(strategyObject);
        this.platirClient = new PlatirClientImpl(this, transactionQueue, marketQueue, queries);
        this.callbackQueue = new StrategyCallbackQueue(annotatedStrategy, queries.getFactory());
        this.loggingHandler = new StrategyLoggingHandler();
        this.logger = createLogger();
    }

    @Override
    public void clearLogs() {
        loggingHandler.clear();
    }

    private Logger createLogger() {
        var newLogger = Logger.getLogger(strategyProfile.getStrategyId());
        newLogger.setUseParentHandlers(false);
        newLogger.addHandler(loggingHandler);
        try {
            var loggingFile = Paths.get(getStrategyCwd().toString(), "logging.txt");
            var fileHandler = new FileHandler(loggingFile.toString());
            fileHandler.setFormatter(new SimpleFormatter());
            newLogger.addHandler(fileHandler);
        } catch (IOException | SecurityException exception) {
            Utils.err().write("Can't add file handler to logging handler: " + exception.getMessage(), exception);
        }
        return newLogger;
    }

    @Override
    public List<LogRecord> getLogs() {
        return loggingHandler.getLogRecords();
    }

    PlatirClientImpl getPlatirClientImpl() {
        return platirClient;
    }

    Path getStrategyCwd() {
        var root = Paths.get(Utils.cwd().toString(), strategyProfile.getStrategyId());
        Utils.dir(root);
        return root;
    }

    void remove() throws StrategyRemovalException {
        try {
            checkIntegrity();
        } catch (IntegrityException e) {
            throw new StrategyRemovalException("Integrity check fails: " + e.getMessage(), e);
        }
        isShutdown.set(true);
        callbackQueue.timedOnDestroy();
        callbackQueue.shutdown();
    }

    void addTransactionContext(TransactionContextImpl transaction) {
        transactions.add(transaction);
    }

    CompositeStrategy getAnnotatedStrategy() {
        return annotatedStrategy;
    }

    void processTick(Tick tick) {
        /* if the strategy is shutdown, no more tick input */
        if (isShutdown.get()) {
            return;
        }
        callbackQueue.push(tick);
    }

    void processBar(Bar bar) {
        /* if the strategy is shutdown, no more bar input */
        if (isShutdown.get()) {
            return;
        }
        callbackQueue.push(bar);
    }

    void processTrade(Trade trade) {
        checkTransactionCompleted();
        callbackQueue.push(trade);
    }

    void processTradeUpdate(TradeUpdate notice) {
        callbackQueue.push(notice);
    }

    Logger getStrategyLogger() {
        return logger;
    }

    private void checkTransactionCompleted() {
        for (var transaction : transactions) {
            if (!checkOrders(transaction)) {
                Utils.err().write("Transaction(" + transaction.getTransaction().getTransactionId() + ") misses some orders.");
                TransactionFacilities.processTradeUpdate(ServiceConstants.CODE_TRANSACTION_ORDERS_MISSING, "orders missing", null, transaction, null);
            }
            var tradedVolume = transaction.successOrders().stream().mapToInt(ctx -> {
                return ctx.getTrades().stream().mapToInt(trade -> trade.getVolume()).sum();
            }).sum();
            var totalVolume = transaction.getTransaction().getVolume();
            if (tradedVolume == totalVolume) {
                /* transaction is completed, awake. */
                transaction.awake();
                /* call strategy callback */
                TransactionFacilities.processTradeUpdate(ServiceConstants.CODE_OK, "completed", null, transaction, null);
            } else if (tradedVolume > totalVolume) {
                /* trade more than expected */
                transaction.awake();
                Utils.err().write("Transaction(" + transaction.getTransaction().getTransactionId() + ") over traded(" + tradedVolume + ">" + totalVolume + ").");
                /* Call strategy callback. */
                TransactionFacilities.processTradeUpdate(ServiceConstants.CODE_TRANSACTION_OVER_TRADE, "over trade", null, transaction, null);
            }
        }
    }

    private boolean checkOrders(TransactionContextImpl transactionContext) {
        var totalOrders = transactionContext.getOrderContexts();
        if (transactionContext.failedOrders().size() + transactionContext.successOrders().size() != totalOrders.size()) {
            return false;
        }
        return totalOrders.containsAll(transactionContext.successOrders()) && totalOrders.containsAll(transactionContext.failedOrders());
    }

    void checkIntegrity() throws IntegrityException {
        for (var transaction : transactions) {
            checkTransactionIntegrity(transaction);
        }
    }

    private void checkTransactionIntegrity(TransactionContextImpl transactionContext) throws IntegrityException {
        var transactionId = transactionContext.getTransaction().getTransactionId();
        var transaction = getTransactionById(transactionId);
        if (transaction == null) {
            throw new IntegrityException("Transaction(" + transactionId + ") not found in data source.");
        }
        if (!Utils.beanEquals(Transaction.class, transactionContext.getTransaction(), transaction)) {
            throw new IntegrityException("Transaction(" + transactionId + ") don't match between data source and runtime.");
        }
        if (!checkOrders(transactionContext))  {
            throw new IntegrityException("Transaction(" + transactionId + ") misses some orders.");
        }
        var orders = platirClient.getOrders(transactionContext.getTransaction().getTransactionId());
        for (var orderContext : transactionContext.getOrderContexts()) {
            var found = false;
            var runtimeOrder = orderContext.getOrder();
            for (var order : orders) {
                if (runtimeOrder.getOrderId().compareTo(order.getOrderId()) == 0) {
                    found = true;
                    checkOrderIntegrity(orderContext, order);
                    break;
                }
            }
            if (!found) {
                throw new IntegrityException("Order(" + runtimeOrder.getOrderId() + ") not found in data source.");
            }
        }
    }

    private Transaction getTransactionById(String transactionId) {
        for (var transaction : platirClient.getTransactions()) {
            if (transaction.getTransactionId().equals(transactionId)) {
                return transaction;
            }
        }
        return null;
    }

    private void checkOrderIntegrity(OrderContext orderContext, Order order) throws IntegrityException {
        var runtimeOrder = orderContext.getOrder();
        if (!Utils.beanEquals(Order.class, runtimeOrder, order)) {
            throw new IntegrityException("Order(" + runtimeOrder.getOrderId() + ") not match between data source and runtime.");
        }
        var trades = platirClient.getTrades(runtimeOrder.getOrderId());
        for (var runtimeTrade : orderContext.getTrades()) {
            var found = false;
            for (var trade : trades) {
                if (trade.getTradeId().compareTo(runtimeTrade.getTradeId()) == 0) {
                    found = true;
                    if (!Utils.beanEquals(Trade.class, runtimeTrade, trade)) {
                        throw new IntegrityException("Trade(" + trade.getTradeId() + ") don't match between data source and runtime.");
                    }
                }
            }
            if (!found) {
                throw new IntegrityException("Trade(" + runtimeTrade.getTradeId() + ") not found in data source.");
            }
        }
    }

    void settle() throws IntegrityException {
        checkIntegrity();
        transactions.clear();
    }

    @Override
    public void start() {
        callbackQueue.timedOnStart(strategyProfile.getArgs(), platirClient);
        isShutdown.set(false);
    }

    @Override
    public void stop(int reason) {
        isShutdown.set(true);
        callbackQueue.timedOnStop(reason);
    }

    @Override
    public StrategyProfile getProfile() {
        return strategyProfile;
    }

    @Override
    public Object getStrategy() {
        return annotatedStrategy.getStrategy();
    }

    @Override
    public PlatirClient getPlatirClient() {
        return getPlatirClientImpl();
    }

    @Override
    public Set<TransactionContext> getTransactionContexts() {
        return new HashSet<>(transactions);
    }

    @Override
    public void interruptTransaction(boolean interrupted) throws InterruptionException {
        platirClient.interrupt(interrupted);
    }
}
