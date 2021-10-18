package io.platir.core.internals;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import io.platir.core.IntegrityException;
import io.platir.core.InvalidLoginException;
import io.platir.core.Platir;
import io.platir.core.PlatirSystem;
import io.platir.core.SettlementException;
import io.platir.core.StrategyCreateException;
import io.platir.core.StrategyRemovalException;
import io.platir.core.StrategyUpdateException;
import io.platir.core.StartupException;
import io.platir.service.api.AdaptorStartupException;
import io.platir.service.StrategyContext;
import io.platir.service.StrategyProfile;
import io.platir.service.Tick;
import io.platir.service.api.DataQueryException;
import io.platir.service.api.MarketAdaptor;
import io.platir.service.api.Queries;
import io.platir.service.api.RiskAssess;
import io.platir.service.api.TradeAdaptor;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

public class PlatirImpl extends Platir {

    private final Lock l = new ReentrantLock();
    private final Condition cond = l.newCondition();
    private final AtomicBoolean isShutdown = new AtomicBoolean(true);
    private RiskAssess rsk;
    private MarketRouter mkRouter;
    private TransactionQueue trQueue;
    private TradeAdaptor trader;
    private MarketAdaptor market;
    private Queries qry;
    private StrategyContextPool stgCtxPool;
    private FileChannel instanceLock;

    @Override
    public void setQueries(Queries queries) {
        qry = queries;
    }

    @Override
    public StrategyContext addStrategy(StrategyProfile profile, Object strategy)
            throws InvalidLoginException, StrategyCreateException {
        return stgCtxPool.add(profile, strategy);
    }

    @Override
    public Set<StrategyContext> getStrategies() {
        return new HashSet<>(stgCtxPool.strategyContexts());
    }

    @Override
    public void join() throws InterruptedException {
        l.lock();
        try {
            cond.await();
        } finally {
            l.unlock();
        }
    }

    @Override
    public void shutdown(int reason) {
        synchronized (isShutdown) {
            if (isShutdown.get()) {
                return;
            }
            /* first shutdown strategies */
            stgCtxPool.shutdown(reason);
            /* then shutdown broker connection */
            market.shutdown();
            trader.shutdown();
            isShutdown.set(true);
            dbDestroy();
            /* signal waiting thread on join() */
            signalJoiner();
            /* release instance lock */
            releaseInstance();
        }
    }

    private void signalJoiner() {
        l.lock();
        try {
            cond.signal();
        } finally {
            l.unlock();
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
            dbInit();
            setup();
            isShutdown.set(false);
        }
    }

    private void acquireInstance() throws StartupException {
        var p = Paths.get(PlatirSystem.cwd().toString(), ".lock");
        if (!Files.exists(p)) {
            PlatirSystem.file(p);
        }
        try {
            instanceLock = FileChannel.open(p, StandardOpenOption.APPEND);
        } catch (IOException ex) {
            throw new StartupException("Two or more instances are not allowed.");
        }
    }

    private void releaseInstance()  {
        try {
            instanceLock.close();
        } catch (IOException ex) {
            /* slient shutdown */
            PlatirSystem.err.write("Can;t release instance lock: " + ex.getMessage(), ex);
        }
    }

    private void dbInit() {
        try {
            qry.initialize();
        } catch (DataQueryException e) {
            throw new RuntimeException("Fail preparing database.", e);
        }
    }

    private void dbDestroy() {
        try {
            qry.destroy();
        } catch (DataQueryException ex) {
            throw new RuntimeException("Fail closing data source.", ex);
        }
    }

    private void setup() throws StartupException {
        try {
            trader.start();
            market.start();
        } catch (AdaptorStartupException e) {
            throw new StartupException("Adaptor startup failure: " + e.getMessage(), e);
        }
        /* if there are subscribed instruments, re-subscribe them. */
        mkRouter.refreshAllSubscriptions();
        if (trQueue == null) {
            trQueue = new TransactionQueue(trader, rsk);
            PlatirSystem.threads.submit(trQueue);
        }
        if (mkRouter == null) {
            mkRouter = new MarketRouter(market, trQueue);
        } else {
            /* need subscribe again after re-login */
            mkRouter.refreshAllSubscriptions();
        }
        if (stgCtxPool == null) {
            stgCtxPool = new StrategyContextPool(trQueue, mkRouter, rsk, qry);
        }
        /* finally initialize strategies when all are ready */
        stgCtxPool.initialize();
    }

    @Override
    public void setTradeAdaptor(TradeAdaptor adaptor) {
        trader = adaptor;
    }

    @Override
    public void setMarketAdaptor(MarketAdaptor adaptor) {
        market = adaptor;
    }

    @Override
    public void setRiskAssess(RiskAssess assess) {
        rsk = assess;
    }

    @Override
    public void settle() throws SettlementException {
        /* need last tick price for settlement price */
        try {
            qry.insert(mkRouter.getLastTicks().toArray(new Tick[1]));
        } catch (DataQueryException e) {
            PlatirSystem.err.write("Fail inserting tick.", e);
        }
        try {
            new Settlement(qry).settle();
        } catch (DataQueryException e) {
            throw new SettlementException("Settlement fails: " + e.getMessage(), e);
        }
        try {
            stgCtxPool.settle();
        } catch (IntegrityException e) {
            throw new SettlementException("Integrity check fails before settlement: " + e.getMessage(), e);
        }
        trQueue.settle();
    }

    @Override
    public void checkIntegrity() throws IntegrityException {
        stgCtxPool.checkIntegrity();
    }

    @Override
    public void updateStrategyProfile(StrategyProfile profile) throws StrategyUpdateException, InvalidLoginException {
        stgCtxPool.update(profile);
    }

    @Override
    public void removeStrategy(StrategyProfile profile) throws StrategyRemovalException, InvalidLoginException {
        stgCtxPool.remove(profile);
    }

}
