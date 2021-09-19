package io.platir.core.internals;

import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;

import io.platir.core.AnnotationParsingException;
import io.platir.core.IntegrityException;
import io.platir.core.InvalidLoginException;
import io.platir.service.Notice;
import io.platir.service.StrategyContext;
import io.platir.service.StrategyDrestroyException;
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

	private final MarketRouter router;
	private final TransactionQueue trQueue;
	private final Queries qry;
	private final Set<StrategyContextImpl> strategies = new ConcurrentSkipListSet<>();

	StrategyContextPool(MarketRouter marketRouter, TransactionQueue transQueue, Queries queries) {
		router = marketRouter;
		trQueue = transQueue;
		qry = queries;
	}

	StrategyContext add(StrategyProfile profile, Object strategy)
			throws AnnotationParsingException, InvalidLoginException {
		var n = verifyLogin(profile);
		if (!n.isGood()) {
			throw new InvalidLoginException("[" + n.getCode() + "]" + n.getMessage());
		}
		/* erase password */
		profile.setPassword("");
		var ctx = new StrategyContextImpl(profile, strategy, trQueue, this, qry);
		/* subscribe instruments */
		router.subscribe(ctx);
		strategies.add(ctx);
		return ctx;
	}

	Set<StrategyContext> getStrategies() {
		return new HashSet<>(strategies);
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

	void remove(StrategyContextImpl strategy) throws StrategyDrestroyException {
		var count = trQueue.countTransactionRunning(strategy);
		if (count > 0) {
			throw new StrategyDrestroyException("The strategy(" + strategy.getPofile().getStrategyId() + ") still has "
					+ count + " transaction running.");
		}
		strategies.remove(strategy);
		router.remove(strategy);
	}

	void settle() throws IntegrityException {
		for (var stg : strategies) {
			stg.settle();
		}
	}
}
