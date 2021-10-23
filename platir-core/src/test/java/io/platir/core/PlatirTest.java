package io.platir.core;

import io.platir.core.internal.PlatirImpl;
import io.platir.core.internal.Simulators;
import io.platir.queries.QueriesImpl;
import io.platir.queries.Utils;
import io.platir.service.Queries;
import io.platir.service.StrategyContext;
import io.platir.service.StrategyProfile;
import io.platir.service.api.MarketAdapter;
import io.platir.service.api.RiskManager;
import io.platir.service.api.TradeAdapter;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 *
 * @author Chen Hongbao
 */
public class PlatirTest {

    private Platir platir;
    private Queries queries;

    @BeforeEach
    public void setUp() throws Exception {
        Utils.delete(Utils.applicationDirectory());

        queries = new QueriesImpl();
        platir = Platir.create();
        platir.setQueries(queries);
        platir.setMarketAdaptor(new Simulators.SimulatedMarketAdapter());
        platir.setTradeAdaptor(new Simulators.SimulatedTradeAdapter(queries));
        platir.setRiskAssess(new Simulators.SimulatedRiskManager(queries));
        
        try {
            platir.start();
        } catch (StartupException exception) {
            throw exception;
        }
    }

    @AfterEach
    public void tearDown() throws Exception {
        Utils.delete(Utils.applicationDirectory());
    }

    @Test
    public void testAddStrategy() throws Exception {
    }

    @Test
    public void testUpdateStrategyProfile() throws Exception {
    }

    @Test
    public void testRemoveStrategy() throws Exception {
    }

    @Test
    public void testSettle() throws Exception {
    }

    @Test
    public void testCheckIntegrity() throws Exception {
    }

}
