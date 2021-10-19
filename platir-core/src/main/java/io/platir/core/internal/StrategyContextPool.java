package io.platir.core.internal;

import io.platir.queries.Utils;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;

import io.platir.core.AnnotationParsingException;
import io.platir.core.IntegrityException;
import io.platir.core.InvalidLoginException;
import io.platir.core.StrategyCreateException;
import io.platir.core.StrategyRemovalException;
import io.platir.core.StrategyUpdateException;
import io.platir.queries.ObjectFactory;
import io.platir.service.Constants;
import io.platir.service.InterruptionException;
import io.platir.service.Notice;
import io.platir.service.StrategyContext;
import io.platir.service.StrategyProfile;
import io.platir.service.DataQueryException;
import io.platir.service.Queries;

/**
 *
 * @author chenh
 *
 */
class StrategyContextPool {

    private final MarketRouter marketRouter;
    private final TransactionQueue transactionQueue;
    private final Queries queries;
    private final Set<StrategyContextImpl> strategies = new ConcurrentSkipListSet<>();

    StrategyContextPool(TransactionQueue transactionQueue, MarketRouter marketQueue, Queries queries) {
        this.marketRouter = marketQueue;
        this.transactionQueue = transactionQueue;
        this.queries = queries;
    }

    StrategyContext add(StrategyProfile strategyProfile, Object strategyObject) throws StrategyCreateException, InvalidLoginException {
        var notice = verifyLogin(strategyProfile);
        if (!notice.isGood()) {
            throw new InvalidLoginException("Can't add strategy on login verification failure(" + notice.getCode() + "): " + notice.getMessage() + ".");
        }
        /* Erase password */
        strategyProfile.setPassword("");
        try {
            var strategyContext = new StrategyContextImpl(strategyProfile, strategyObject, transactionQueue, marketRouter, queries);
            setStrategyCreated(strategyProfile);
            /* Subscribe instruments. */
            marketRouter.subscribe(strategyContext);
            strategies.add(strategyContext);
            return strategyContext;
        } catch (AnnotationParsingException exception) {
            throw new StrategyCreateException("Strategy parsing failure: " + exception.getMessage() + ".", exception);
        }
    }

    private void setStrategyCreated(StrategyProfile profile) throws StrategyCreateException {
        /* Insert strategy profile into data source. */
        profile.setState("running");
        profile.setCreateDate(Utils.date());
        try {
            queries.insert(profile);
        } catch (DataQueryException exception) {
            throw new StrategyCreateException("Can't create strategy(" + profile.getStrategyId() + ") profile: " + exception.getMessage() + ".", exception);
        }
    }

    Set<StrategyContextImpl> strategyContexts() {
        return strategies;
    }

    void initialize() {
        strategies.forEach(strategy -> {
            strategy.initialize();
        });
    }

    void shutdown(int reason) {
        strategies.forEach(strategy -> {
            strategy.shutdown(reason);
        });
    }

    private Notice verifyLogin(StrategyProfile profile) {
        var notice = ObjectFactory.newNotice();
        notice.setCode(Constants.CODE_OK);
        notice.setMessage("good");
        try {
            var found = false;
            for (var user : queries.selectUsers()) {
                if (user.getUserId().equals(profile.getStrategyId()) && user.getPassword().equals(profile.getPassword())) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                notice.setCode(Constants.CODE_LOGIN_FAIL);
                notice.setMessage("login failure");
            }
        } catch (DataQueryException exception) {
            notice.setCode(Constants.CODE_LOGIN_QUERY_FAIL);
            notice.setMessage("login SQL operation failure");
            notice.setError(exception);
        }
        return notice;
    }

    void checkIntegrity() throws IntegrityException {
        for (var strategy : strategies) {
            strategy.checkIntegrity();
        }
    }

    void remove(StrategyProfile profile) throws StrategyRemovalException, InvalidLoginException {
        /* Check precondition for removal. */
        var notice = verifyLogin(profile);
        if (!notice.isGood()) {
            throw new InvalidLoginException("Identity check failure(" + notice.getCode() + ") on removal: " + notice.getMessage() + ".");
        }
        var strategy = findStrategyContext(profile);
        if (strategy == null) {
            throw new StrategyRemovalException("Can't find strategy(" + profile.getStrategyId() + ") context.");
        }
        try {
            /* Client can't open/close position as it is being removed. */
            strategy.getPlatirClientImpl().interrupt(true);
        } catch (InterruptionException ex) {
            throw new StrategyRemovalException("Can't interrupt client before strategy(" + strategy.getProfile().getStrategyId() + ") removal.");
        }
        var count = transactionQueue.countTransactionRunning(strategy);
        if (count > 0) {
            try {
                /* Restore interrupt state as it can't be removed */
                strategy.getPlatirClientImpl().interrupt(false);
            } catch (InterruptionException ex) {
                throw new StrategyRemovalException("Can't restore client interrupt state for strategy(" + strategy.getProfile().getStrategyId() + ") removal.");
            }
            throw new StrategyRemovalException("The strategy(" + strategy.getProfile().getStrategyId() + ") still has " + count + " transaction running.");
        }
        /* Remove the strategy. */
        strategy.remove();
        strategies.remove(strategy);
        marketRouter.removeSubscription(strategy);
        setStrategyRemoved(profile);
    }

    private void setStrategyRemoved(StrategyProfile profile) throws StrategyRemovalException {
        /* Set strategy state in data source, to be removed at settlement. */
        profile.setState("removed");
        profile.setRemoveDate(Utils.date());
        try {
            queries.update(profile);
        } catch (DataQueryException exception) {
            throw new StrategyRemovalException("Can't update strategy(" + profile.getStrategyId() + ") profile: " + exception.getMessage() + ".", exception);
        }
    }

    private StrategyContextImpl findStrategyContext(StrategyProfile profile) {
        for (var strategy : strategies) {
            if (strategy.getProfile().getStrategyId().equals(profile.getStrategyId())) {
                return strategy;
            }
        }
        return null;
    }

    void update(StrategyProfile profile) throws StrategyUpdateException, InvalidLoginException {
        var notice = verifyLogin(profile);
        if (!notice.isGood()) {
            throw new InvalidLoginException("Identity check failure(" + notice.getCode() + ") on update profile: " + notice.getMessage() + ".");
        }
        StrategyContextImpl strategyContext = null;
        for (var existingContext : strategyContexts()) {
            if (existingContext.getProfile().getStrategyId().equals(profile.getStrategyId())) {
                strategyContext = existingContext;
                break;
            }
        }
        if (strategyContext == null) {
            throw new StrategyUpdateException("Strategy(" + profile.getStrategyId() + ") not found in pool.");
        }
        update(strategyContext.getProfile(), profile);
        marketRouter.updateSubscription(strategyContext);
    }

    private void update(StrategyProfile previousProfile, StrategyProfile newProfile) throws StrategyUpdateException {
        try {
            /* Only argument and subscription are changed. */
            previousProfile.setArgs(newProfile.getArgs());
            previousProfile.setInstrumentIds(newProfile.getInstrumentIds());
            /* Update data source. */
            queries.update(previousProfile);
        } catch (DataQueryException e) {
            throw new StrategyUpdateException("Can't update strategy(" + previousProfile.getStrategyId()
                    + ") profile in data source: " + e.getMessage(), e);
        }
    }

    void settle() throws IntegrityException {
        for (var strategy : strategies) {
            strategy.settle();
        }
    }
}
