package io.platir.core.internals;

import java.sql.SQLException;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;

import io.platir.core.AnnotationParsingException;
import io.platir.core.IntegrityException;
import io.platir.core.InvalidLoginException;
import io.platir.core.StrategyCreateException;
import io.platir.core.StrategyRemovalException;
import io.platir.core.StrategyUpdateException;
import io.platir.service.Notice;
import io.platir.service.StrategyContext;
import io.platir.service.StrategyProfile;
import io.platir.service.api.Queries;

/**
 * Error code explanation:
 * <ul>
 * <li>5001: Login failure.
 * <li>5002: Login SQL operation failure.
 * </ul>
 * 
 * @author chenh
 *
 */
class StrategyContextPool {

	private final MarketRouter market;
	private final TransactionQueue trader;
	private final Queries qry;
	private final Set<StrategyContextImpl> strategies = new ConcurrentSkipListSet<>();

	StrategyContextPool(TransactionQueue trQueue, MarketRouter mkRouter, Queries queries) {
		market = mkRouter;
		trader = trQueue;
		qry = queries;
	}

	StrategyContext add(StrategyProfile profile, Object strategy)
			throws StrategyCreateException, InvalidLoginException {
		var n = verifyLogin(profile);
		if (!n.isGood()) {
			throw new InvalidLoginException(
					"Can't add strategy on login verification failure(" + n.getCode() + "): " + n.getMessage() + ".");
		}
		/* erase password */
		profile.setPassword("");

		try {
			var ctx = new StrategyContextImpl(profile, strategy, trader, market, qry);
			/* insert strategy profile into data source */
			qry.insert(profile);
			/* subscribe instruments */
			market.subscribe(ctx);
			strategies.add(ctx);
			return ctx;
		} catch (AnnotationParsingException e) {
			throw new StrategyCreateException("Strategy parsing failure: " + e.getMessage() + ".", e);
		} catch (SQLException e) {
			throw new StrategyCreateException("Can't save strategy profile: " + e.getMessage() + ".", e);
		}
	}

	Set<StrategyContextImpl> strategyContexts() {
		return strategies;
	}

	void initialize() {
		strategies.forEach(stg -> {
			stg.initialize();
		});
	}

	void shutdown(int reason) {
		strategies.forEach(stg -> {
			stg.shutdown(reason);
		});
	}

	private Notice verifyLogin(StrategyProfile profile) {
		var n = new Notice();
		n.setCode(0);
		n.setMessage("good");
		try {
			var found = false;
			for (var u : qry.selectUsers()) {
				if (u.getUserId().equals(profile.getStrategyId()) && u.getPassword().equals(profile.getPassword())) {
					found = true;
					break;
				}
			}
			if (!found) {
				n.setCode(5001);
				n.setMessage("login failure");
			}
		} catch (SQLException e) {
			n.setCode(5002);
			n.setMessage("login SQL operation failure");
		}
		return n;
	}

	void checkIntegrity() throws IntegrityException {
		for (var stg : strategies) {
			stg.checkIntegrity();
		}
	}

	void remove(StrategyProfile profile) throws StrategyRemovalException, InvalidLoginException {
		/* check precondition for removal */
		var r = verifyLogin(profile);
		if (!r.isGood()) {
			throw new InvalidLoginException(
					"Identity check failure(" + r.getCode() + ") on removal: " + r.getMessage() + ".");
		}
		var strategy = findStrategyContext(profile);
		if (strategy == null) {
			throw new StrategyRemovalException("Can't find strategy(" + profile.getStrategyId() + ") context.");
		}
		var count = trader.countTransactionRunning(strategy);
		if (count > 0) {
			throw new StrategyRemovalException("The strategy(" + strategy.getProfile().getStrategyId() + ") still has "
					+ count + " transaction running.");
		}
		/* remove the strategy */
		strategy.remove();
		strategies.remove(strategy);
		market.removeSubscription(strategy);
	}

	private StrategyContextImpl findStrategyContext(StrategyProfile profile) {
		for (var stg : strategies) {
			if (stg.getProfile().getStrategyId().equals(profile.getStrategyId())) {
				return stg;
			}
		}
		return null;
	}

	void update(StrategyProfile profile) throws StrategyUpdateException, InvalidLoginException {
		var r = verifyLogin(profile);
		if (!r.isGood()) {
			throw new InvalidLoginException(
					"Identity check failure(" + r.getCode() + ") on update profile: " + r.getMessage() + ".");
		}
		StrategyContextImpl ctx = null;
		for (var stg : strategyContexts()) {
			if (stg.getProfile().getStrategyId().equals(profile.getStrategyId())) {
				ctx = stg;
				break;
			}
		}
		if (ctx == null) {
			throw new StrategyUpdateException("Strategy(" + profile.getStrategyId() + ") not found in pool.");
		}
		update(ctx.getProfile(), profile);
		market.updateSubscription(ctx);
	}

	private void update(StrategyProfile old, StrategyProfile newProf) {
		/* strategy ID, user information don't change */
		old.setArgs(newProf.getArgs());
		old.setInstrumentIds(newProf.getInstrumentIds());
	}

	void settle() throws IntegrityException {
		for (var stg : strategies) {
			stg.settle();
		}
	}
}
