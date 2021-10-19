package io.platir.core;

import java.util.Set;

import io.platir.core.internal.PlatirImpl;
import io.platir.service.StrategyContext;
import io.platir.service.StrategyProfile;
import io.platir.service.api.MarketAdaptor;
import io.platir.service.Queries;
import io.platir.service.api.TradeAdaptor;
import io.platir.service.api.RiskManager;

public abstract class Platir {

    public static Platir create() {
        return new PlatirImpl();
    }

    public abstract void setQueries(Queries queries);

    public abstract void setRiskAssess(RiskManager assess);

    public abstract void setTradeAdaptor(TradeAdaptor trade);

    public abstract void setMarketAdaptor(MarketAdaptor market);

    public abstract StrategyContext addStrategy(StrategyProfile profile, Object strategy)
            throws StrategyCreateException, InvalidLoginException;

    public abstract void updateStrategyProfile(StrategyProfile profile)
            throws StrategyUpdateException, InvalidLoginException;

    public abstract void removeStrategy(StrategyProfile profile) throws StrategyRemovalException, InvalidLoginException;

    public abstract Set<StrategyContext> getStrategies();

    public abstract void join() throws InterruptedException;

    public abstract void start() throws StartupException;

    public abstract void shutdown(int reason);

    public abstract void settle() throws SettlementException;

    public abstract void checkIntegrity() throws IntegrityException;
}
