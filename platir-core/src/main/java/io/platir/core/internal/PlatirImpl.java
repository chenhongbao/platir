package io.platir.core.internal;

import io.platir.queries.Utils;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import io.platir.core.IntegrityException;
import io.platir.core.InvalidLoginException;
import io.platir.core.Platir;
import io.platir.core.SettlementException;
import io.platir.core.StrategyCreateException;
import io.platir.core.StrategyRemovalException;
import io.platir.core.StrategyUpdateException;
import io.platir.core.StartupException;
import io.platir.service.StrategyContext;
import io.platir.service.StrategyProfile;
import io.platir.service.Tick;
import io.platir.service.DataQueryException;
import io.platir.service.Queries;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import io.platir.service.api.RiskManager;
import io.platir.service.api.TradeAdapter;
import io.platir.service.api.MarketAdapter;
import io.platir.service.api.ApiConstants;

public class PlatirImpl extends Platir {

    private final AtomicBoolean isShutdown = new AtomicBoolean(true);
    private Queries queries;
    private RiskManager riskManager;
    private MarketRouter marketRouter;
    private TransactionQueue transactionQueue;
    private TradeAdapter tradeAdaptor;
    private MarketAdapter marketAdaptor;
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
    public void setQueries(Queries queries) {
        this.queries = queries;
    }

    @Override
    public void start() throws StartupException {
        synchronized (isShutdown) {
            if (!isShutdown.get()) {
                return;
            }
            /* ensure single instance */
            acquireInstance();
            setup();
            queriesInit();
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

    private void queriesInit() {
        try {
            queries.initialize();
        } catch (DataQueryException e) {
            throw new RuntimeException("Fail preparing database.", e);
        }
    }

    private void setup() throws StartupException {
        int code;
        code = tradeAdaptor.start();
        if (code != ApiConstants.CODE_OK) {
            throw new StartupException("Trader adapter startup failure: " + code + ".");
        }
        code = marketAdaptor.start();
        if (code != ApiConstants.CODE_OK) {
            throw new StartupException("Market adapter startup failure: " + code + ".");
        }
        if (transactionQueue == null) {
            transactionQueue = new TransactionQueue(tradeAdaptor, riskManager, queries.getFactory());
            Utils.threads().submit(transactionQueue);
        }
        if (marketRouter == null) {
            marketRouter = new MarketRouter(marketAdaptor, transactionQueue, queries);
        } else {
            /* Need subscribe again after re-login. */
            marketRouter.refreshAllSubscriptions();
        }
        if (strategyContextPool == null) {
            strategyContextPool = new StrategyContextPool(transactionQueue, marketRouter, queries);
        }
        /* Finally initialize strategies when all are ready. */
        strategyContextPool.start();
    }

    @Override
    public void setTradeAdaptor(TradeAdapter adaptor) {
        tradeAdaptor = adaptor;
    }

    @Override
    public void setMarketAdaptor(MarketAdapter adaptor) {
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
            Utils.err().write("Fail inserting tick.", exception);
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
