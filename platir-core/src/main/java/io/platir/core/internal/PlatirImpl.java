package io.platir.core.internal;

import io.platir.queries.Utils;
import io.platir.queries.QueriesImpl;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import io.platir.core.IntegrityException;
import io.platir.core.InvalidLoginException;
import io.platir.core.Platir;
import io.platir.core.SettlementException;
import io.platir.core.StrategyCreateException;
import io.platir.core.StrategyRemovalException;
import io.platir.core.StrategyUpdateException;
import io.platir.core.StartupException;
import io.platir.service.api.AdaptorStartupException;
import io.platir.service.StrategyContext;
import io.platir.service.StrategyProfile;
import io.platir.service.Tick;
import io.platir.service.DataQueryException;
import io.platir.service.api.MarketAdaptor;
import io.platir.service.Queries;
import io.platir.service.api.TradeAdaptor;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import io.platir.service.api.RiskManager;

public class PlatirImpl extends Platir {

    private final Lock shutdownLock = new ReentrantLock();
    private final Condition shutdownCondition = shutdownLock.newCondition();
    private final AtomicBoolean isShutdown = new AtomicBoolean(true);
    private Queries queries;
    private RiskManager riskManager;
    private MarketRouter marketRouter;
    private TransactionQueue transactionQueue;
    private TradeAdaptor tradeAdaptor;
    private MarketAdaptor marketAdaptor;
    private StrategyContextPool strategyContextPool;
    private FileChannel instanceLock;

    @Override
    public StrategyContext addStrategy(StrategyProfile strategyProfile, Object strategyObject) throws InvalidLoginException, StrategyCreateException {
        return strategyContextPool.add(strategyProfile, strategyObject);
    }

    @Override
    public Set<StrategyContext> getStrategies() {
        return new HashSet<>(strategyContextPool.strategyContexts());
    }

    @Override
    public void join() throws InterruptedException {
        shutdownLock.lock();
        try {
            shutdownCondition.await();
        } finally {
            shutdownLock.unlock();
        }
    }

    @Override
    public void shutdown(int reason) {
        synchronized (isShutdown) {
            if (isShutdown.get()) {
                return;
            }
            /* first shutdown strategies */
            strategyContextPool.shutdown(reason);
            /* then shutdown broker connection */
            marketAdaptor.shutdown();
            tradeAdaptor.shutdown();
            isShutdown.set(true);
            queriesDestroy();
            /* signal waiting thread on join() */
            signalJoiner();
            /* release instance lock */
            releaseInstance();
        }
    }

    @Override
    public void setQueries(Queries queries) {
        this.queries = queries;
    }

    private void signalJoiner() {
        shutdownLock.lock();
        try {
            shutdownCondition.signal();
        } finally {
            shutdownLock.unlock();
        }
    }

    @Override
    public void start() throws StartupException {
        synchronized (isShutdown) {
            if (!isShutdown.get()) {
                return;
            }
            /* ensure single instance */
            acquireInstance();
            queriesInit();
            setup();
            isShutdown.set(false);
        }
    }

    private void acquireInstance() throws StartupException {
        var instanceLockingFile = Paths.get(Utils.cwd().toString(), ".lock");
        if (!Files.exists(instanceLockingFile)) {
            Utils.file(instanceLockingFile);
        }
        try {
            instanceLock = FileChannel.open(instanceLockingFile, StandardOpenOption.APPEND);
        } catch (IOException ex) {
            throw new StartupException("Two or more instances are not allowed.");
        }
    }

    private void releaseInstance() {
        try {
            instanceLock.close();
        } catch (IOException ex) {
            /* slient shutdown */
            Utils.err.write("Can;t release instance lock: " + ex.getMessage(), ex);
        }
    }

    private void queriesInit() {
        try {
            queries.initialize();
        } catch (DataQueryException e) {
            throw new RuntimeException("Fail preparing database.", e);
        }
    }

    private void queriesDestroy() {
        try {
            queries.destroy();
        } catch (DataQueryException ex) {
            throw new RuntimeException("Fail closing data source.", ex);
        }
    }

    private void setup() throws StartupException {
        try {
            tradeAdaptor.start();
            marketAdaptor.start();
        } catch (AdaptorStartupException e) {
            throw new StartupException("Adaptor startup failure: " + e.getMessage(), e);
        }
        if (transactionQueue == null) {
            transactionQueue = new TransactionQueue(tradeAdaptor, riskManager, queries.getFactory());
            Utils.threads.submit(transactionQueue);
        }
        if (marketRouter == null) {
            marketRouter = new MarketRouter(marketAdaptor, transactionQueue);
        } else {
            /* Need subscribe again after re-login. */
            marketRouter.refreshAllSubscriptions();
        }
        if (strategyContextPool == null) {
            strategyContextPool = new StrategyContextPool(transactionQueue, marketRouter, queries);
        }
        /* Finally initialize strategies when all are ready. */
        strategyContextPool.initialize();
    }

    @Override
    public void setTradeAdaptor(TradeAdaptor adaptor) {
        tradeAdaptor = adaptor;
    }

    @Override
    public void setMarketAdaptor(MarketAdaptor adaptor) {
        marketAdaptor = adaptor;
    }

    @Override
    public void setRiskAssess(RiskManager assess) {
        riskManager = assess;
    }

    @Override
    public void settle() throws SettlementException {
        /* Need last tick price for settlement price. */
        try {
            queries.insert(marketRouter.getLastTicks().toArray(new Tick[1]));
        } catch (DataQueryException exception) {
            Utils.err.write("Fail inserting tick.", exception);
        }
        try {
            new Settlement(queries).settle();
        } catch (DataQueryException exception) {
            throw new SettlementException("Settlement fails: " + exception.getMessage(), exception);
        }
        try {
            strategyContextPool.settle();
        } catch (IntegrityException exception) {
            throw new SettlementException("Integrity check fails before settlement: " + exception.getMessage(), exception);
        }
        transactionQueue.settle();
    }

    @Override
    public void checkIntegrity() throws IntegrityException {
        strategyContextPool.checkIntegrity();
    }

    @Override
    public void updateStrategyProfile(StrategyProfile profile) throws StrategyUpdateException, InvalidLoginException {
        strategyContextPool.update(profile);
    }

    @Override
    public void removeStrategy(StrategyProfile profile) throws StrategyRemovalException, InvalidLoginException {
        strategyContextPool.remove(profile);
    }

}
