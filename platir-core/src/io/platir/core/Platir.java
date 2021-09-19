package io.platir.core;

import java.util.Set;

import io.platir.core.internals.PlatirImpl;
import io.platir.service.StrategyContext;
import io.platir.service.StrategyProfile;
import io.platir.service.api.MarketAdaptor;
import io.platir.service.api.Queries;
import io.platir.service.api.RiskAssess;
import io.platir.service.api.TradeAdaptor;

public abstract class Platir {
	public static Platir create() {
		return new PlatirImpl();
	}

	public abstract void setRiskAssess(RiskAssess assess);

	public abstract void setTradeAdaptor(TradeAdaptor trade);

	public abstract void setMarketAdaptor(MarketAdaptor market);

	public abstract void setQueries(Queries queries);

	public abstract StrategyContext addStrategy(StrategyProfile profile, Object strategy)
			throws AnnotationParsingException, InvalidLoginException;

	public abstract Set<StrategyContext> getStrategies();

	public abstract void join() throws InterruptedException;

	public abstract void start();

	public abstract void shutdown(int reason);
	
	public abstract void settle() throws SettlementException;
	
	public abstract void checkIntegrity() throws IntegrityException;
}
