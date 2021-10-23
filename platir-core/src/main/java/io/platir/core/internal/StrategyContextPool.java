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
import io.platir.service.InterruptionException;
import io.platir.service.StrategyContext;
import io.platir.service.StrategyProfile;
import io.platir.service.DataQueryException;
import io.platir.service.Queries;
import io.platir.service.ServiceConstants;

/**
 *
 * @author Chen Hongbao
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
        var returnCode = verifyLogin(strategyProfile);
        if (returnCode != ServiceConstants.CODE_OK) {
            throw new InvalidLoginException("Can't add strategy on login verification failure(" + returnCode + ").");
        }
        /* Erase password */
        strategyProfile.setPassword("");
        try {
            var strategyContext = new StrategyContextImpl(strategyProfile, strategyObject, transactionQueue, marketRouter, queries);
            setStrategyCreated(strategyProfile);
            /* Subscribe instruments. */
            marketRouter.subscribe(strategyContext);
            strategies.add(strategyContext);
            /* Call onStart() method. */
            strategyContext.start();
            return strategyContext;
        } catch (AnnotationParsingException exception) {
            throw new StrategyCreateException("Strategy parsing failure: " + exception.getMessage() + ".", exception);
        }
    }

    private void setStrategyCreated(StrategyProfile profile) throws StrategyCreateException {
        /* Insert strategy profile into data source. */
        profile.setState(ServiceConstants.FLAG_STRATEGY_RUNNING);
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

    void stop(int reason) {
        strategies.forEach(strategy -> {
            strategy.stop(reason);
        });
    }

    private int verifyLogin(StrategyProfile profile) {
        try {
            var found = false;
            for (var user : queries.selectUsers()) {
                if (user.getUserId().equals(profile.getStrategyId()) && user.getPassword().equals(profile.getPassword())) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                return ServiceConstants.CODE_LOGIN_FAIL;
            } else {
                return ServiceConstants.CODE_OK;
            }
        } catch (DataQueryException exception) {
            return ServiceConstants.CODE_LOGIN_QUERY_FAIL;
        }
    }

    void checkIntegrity() throws IntegrityException {
        for (var strategy : strategies) {
            strategy.checkIntegrity();
        }
    }

    void remove(StrategyProfile profile) throws StrategyRemovalException, InvalidLoginException {
        /* Check precondition for removal. */
        var returnCode = verifyLogin(profile);
        if (returnCode != ServiceConstants.CODE_OK) {
            throw new InvalidLoginException("Identity check failure(" + returnCode + ") on removal.");
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
        var count = transactionQueue.countOnlineTransactions(strategy);
        if (count > 0) {
            try {
                /* Restore interrupt state as it can't be removed */
                strategy.getPlatirClientImpl().interrupt(false);
            } catch (InterruptionException ex) {
                throw new StrategyRemovalException("Can't restore client interrupt state for strategy(" + strategy.getProfile().getStrategyId() + ") removal.");
            }
            throw new StrategyRemovalException("The strategy(" + strategy.getProfile().getStrategyId() + ") still has " + count + " transaction online.");
        }
        /* Remove the strategy. */
        strategy.remove();
        strategies.remove(strategy);
        marketRouter.removeSubscription(strategy);
        setStrategyRemoved(profile);
    }

    private void setStrategyRemoved(StrategyProfile profile) throws StrategyRemovalException {
        /* Set strategy state in data source, to be removed at settlement. */
        profile.setState(ServiceConstants.FLAG_STRATEGY_REMOVED);
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
        var returnCode = verifyLogin(profile);
        if (returnCode != ServiceConstants.CODE_OK) {
            throw new InvalidLoginException("Identity check failure(" + returnCode + ") on update profile.");
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
        update(strategyContext, profile);
        marketRouter.updateSubscription(strategyContext);
    }

    private void update(StrategyContextImpl strategyContext, StrategyProfile newProfile) throws StrategyUpdateException {
        var exsistingProfile = strategyContext.getProfile();
        try {
            /* Only argument and subscription are changed. */
            exsistingProfile.setArgs(newProfile.getArgs());
            exsistingProfile.setInstrumentIds(newProfile.getInstrumentIds());
            /* Update data source. */
            queries.update(exsistingProfile);
        } catch (DataQueryException e) {
            throw new StrategyUpdateException("Can't update strategy(" + exsistingProfile.getStrategyId() + ") profile in data source: " + e.getMessage(), e);
        }
    }

    void settle() throws IntegrityException {
        for (var strategy : strategies) {
            strategy.settle();
        }
    }
}
