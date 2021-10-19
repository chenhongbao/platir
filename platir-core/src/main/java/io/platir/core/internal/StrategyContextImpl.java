package io.platir.core.internal;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.atomic.AtomicBoolean;

import io.platir.core.AnnotationParsingException;
import io.platir.core.IntegrityException;
import io.platir.core.StrategyRemovalException;
import io.platir.core.internal.objects.ObjectFactory;
import io.platir.service.Bar;
import io.platir.service.InterruptionException;
import io.platir.service.Notice;
import io.platir.service.Order;
import io.platir.service.OrderContext;
import io.platir.service.PlatirClient;
import io.platir.service.StrategyContext;
import io.platir.service.StrategyProfile;
import io.platir.service.Tick;
import io.platir.service.Trade;
import io.platir.service.Transaction;
import io.platir.service.TransactionContext;
import io.platir.service.DataQueryException;
import io.platir.service.Queries;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.logging.FileHandler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

/**
 * Error code explanation:
 * <ul>
 * <li>4001: Callback throws exception.</li>
 * <li>4002: Callback is timeout.</li>
 * <li>4003: transaction over traded.</li>
 * </ul>
 *
 * @author Chen Hongbao
 * @since 1.0.0
 */
class StrategyContextImpl implements StrategyContext {

    private final Logger logger;
    private final StrategyProfile strategyProfile;
    private final AnnotatedStrategy annotatedStrategy;
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
        this.annotatedStrategy = new AnnotatedStrategy(strategyObject);
        this.platirClient = new PlatirClientImpl(this, transactionQueue, marketQueue, queries);
        this.callbackQueue = new StrategyCallbackQueue(annotatedStrategy);
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
            Utils.err.write("Can't add file handler to logging handler: " + exception.getMessage(), exception);
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

    AnnotatedStrategy getAnnotatedStrategy() {
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
        callbackQueue.push(callbackQueue);
    }

    void processNotice(Notice notice) {
        callbackQueue.push(notice);
    }

    Logger getStrategyLogger() {
        return logger;
    }

    private void checkTransactionCompleted() {
        for (var transaction : transactions) {
            var tradedVolume = transaction.getOrderContexts().stream().mapToInt(ctx -> {
                return ctx.getTrades().stream().mapToInt(trade -> trade.getVolume()).sum();
            }).sum();
            var totalVolume = transaction.getTransaction().getVolume();
            if (tradedVolume == totalVolume) {
                /* transaction is completed, awake. */
                transaction.awake();
                /* call strategy callback */
                simpleNotice(0, "Completed");
            } else if (tradedVolume > totalVolume) {
                /* trade more than expected */
                transaction.awake();
                Utils.err.write("Transaction(" + transaction.getTransaction().getTransactionId() + ") over traded(" + tradedVolume + ">" + totalVolume + ").");
                /* call strategy callback */
                simpleNotice(4003, "Over traded");
            }
        }
    }

    private void simpleNotice(int code, String message) {
        var notice = ObjectFactory.newNotice();
        notice.setCode(code);
        notice.setMessage(message);
        processNotice(notice);
    }

    void checkIntegrity() throws IntegrityException {
        for (var transaction : transactions) {
            checkTransactionIntegrity(transaction);
        }
    }

    private void checkTransactionIntegrity(TransactionContextImpl t) throws IntegrityException {
        var transactionId = t.getTransaction().getTransactionId();
        var transaction = getTransactionById(transactionId);
        if (transaction == null) {
            throw new IntegrityException("Transaction(" + transactionId + ") not found in data source.");
        }
        if (!equals(t.getTransaction(), transaction)) {
            throw new IntegrityException("Transaction(" + transactionId + ") don't match between data source and runtime.");
        }
        var orders = platirClient.getOrders(t.getTransaction().getTransactionId());
        for (var orderContext : t.getOrderContexts()) {
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
        if (!equals(runtimeOrder, order)) {
            throw new IntegrityException("Order(" + runtimeOrder.getOrderId() + ") not match between data source and runtime.");
        }
        var trades = platirClient.getTrades(runtimeOrder.getOrderId());
        for (var runtimeTrade : orderContext.getTrades()) {
            var found = false;
            for (var trade : trades) {
                if (trade.getTradeId().compareTo(runtimeTrade.getTradeId()) == 0) {
                    found = true;
                    if (!equals(runtimeTrade, trade)) {
                        throw new IntegrityException("Trade(" + trade.getTradeId() + ") don't match between data source and runtime.");
                    }
                }
            }
            if (!found) {
                throw new IntegrityException("Trade(" + runtimeTrade.getTradeId() + ") not found in data source.");
            }
        }
    }

    private boolean equals(Transaction t0, Transaction t1) {
        return t0.getTransactionId().equals(t1.getTransactionId()) && t0.getStrategyId().equals(t1.getStrategyId())
                && t0.getInstrumentId().equals(t1.getInstrumentId()) && t0.getPrice().equals(t1.getPrice())
                && t0.getVolume().equals(t1.getVolume()) && t0.getOffset().equals(t1.getOffset())
                && t0.getDirection().equals(t1.getDirection()) && t0.getState().equals(t1.getState())
                && t0.getStateMessage().equals(t1.getStateMessage()) && t0.getTradingDay().equals(t1.getTradingDay())
                && t0.getUpdateTime().equals(t1.getUpdateTime());
    }

    private boolean equals(Order o1, Order o2) {
        return o1.getOrderId().equals(o2.getOrderId()) && o1.getTransactionId().equals(o2.getTransactionId())
                && o1.getInstrumentId().equals(o2.getInstrumentId()) && o1.getPrice().equals(o2.getPrice())
                && o1.getVolume().equals(o2.getVolume()) && o1.getDirection().equals(o2.getDirection())
                && o1.getOffset().equals(o2.getOffset()) && o1.getTradingDay().equals(o2.getTradingDay());
    }

    private boolean equals(Trade tr0, Trade tr1) {
        return tr0.getTradeId().equals(tr1.getTradeId()) && tr0.getOrderId().equals(tr1.getOrderId())
                && tr0.getInstrumentId().equals(tr1.getInstrumentId()) && tr0.getPrice().equals(tr1.getPrice())
                && tr0.getVolume().equals(tr1.getVolume()) && tr0.getDirection().equals(tr1.getDirection())
                && tr0.getTradingDay().equals(tr1.getTradingDay()) && tr0.getUpdateTime().equals(tr1.getUpdateTime());
    }

    void settle() throws IntegrityException {
        checkIntegrity();
        transactions.clear();
    }

    @Override
    public void initialize() {
        callbackQueue.timedOnStart(strategyProfile.getArgs(), platirClient);
        isShutdown.set(false);
    }

    @Override
    public void shutdown(int reason) {
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
