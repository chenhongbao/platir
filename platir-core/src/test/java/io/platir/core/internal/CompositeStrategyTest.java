package io.platir.core.internal;

import io.platir.core.AnnotationParsingException;
import io.platir.queries.FactoryImpl;
import io.platir.service.Factory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 *
 * @author Chen Hongbao
 */
public class CompositeStrategyTest {

    private CompositeStrategy composite;
    private SuperStrategy strategy;
    private final Factory factory;

    public CompositeStrategyTest() {
        factory = new FactoryImpl();
    }

    @BeforeEach
    public void setUp() throws AnnotationParsingException {
        strategy = new SuperStrategy();
        composite = new CompositeStrategy(strategy);
        composite.onStart(new String[]{"hello"}, null);
        /* Invoke start callback twice. */
        assertEquals(2, strategy.startCounts().size(), "Start callback not hit.");
    }

    @AfterEach
    public void tearDown() {
        composite.onStop(0);
        composite.onDestroy();
        /* Invoke stop/destroy callback twice. */
        assertEquals(2, strategy.stopCounts().size(), "Stop callback not hit.");
        assertEquals(2, strategy.destroyCounts().size(), "Destroy callback not hit.");
    }

    @Test
    public void testOnTrade() {
        var trade = factory.newTrade();
        trade.setInstrumentId("rb2210");
        composite.onTrade(trade);
        /* Invoke twice on the same trade. */
        assertEquals(1, strategy.tradeCounts().size(), "Trade update count wrong.");
        var counts = strategy.tradeCounts().get(trade);
        assertTrue(counts != null, "Trade counts not found.");
        assertTrue(counts.contains("onTrade()"), "onTrade() not hit");
        assertTrue(counts.contains("@OnTrade"), "@OnTrade not hit.");
    }

    /**
     * Test of onTick method, of class AnnotatedStrategy.
     */
    @Test
    public void testOnTick() {
        var tick0 = factory.newTick();
        tick0.setInstrumentId("c2201");
        var tick1 = factory.newTick();
        tick1.setInstrumentId("c2205");
        var tick2 = factory.newTick();
        tick2.setInstrumentId("rb2210");
        var tick3 = factory.newTick();
        tick3.setInstrumentId("c2209");
        composite.onTick(tick0);
        composite.onTick(tick1);
        composite.onTick(tick2);
        composite.onTick(tick3);
        assertEquals(4, strategy.tickCounts().size(), "Tick callback not hit.");
        assertEquals(1, strategy.tickCounts().get(tick0).size(), "Tick callback not hit.");
        assertEquals(2, strategy.tickCounts().get(tick1).size(), "Tick callback not hit.");
        assertEquals(2, strategy.tickCounts().get(tick2).size(), "Tick callback not hit.");
        assertEquals(2, strategy.tickCounts().get(tick3).size(), "Tick callback not hit.");
    }

    /**
     * Test of onBar method, of class AnnotatedStrategy.
     */
    @Test
    public void testOnBar() {
        var bar0 = factory.newBar();
        bar0.setInstrumentId("rb2210");
        bar0.setMinute(1);
        var bar1 = factory.newBar();
        bar1.setInstrumentId("rb2210");
        bar1.setMinute(5);
        var bar2 = factory.newBar();
        bar2.setInstrumentId("rb2210");
        bar2.setMinute(10);
        var bar3 = factory.newBar();
        bar3.setInstrumentId("c2201");
        bar3.setMinute(1);
        var bar4 = factory.newBar();
        bar4.setInstrumentId("c2201");
        bar4.setMinute(15);
        var bar5 = factory.newBar();
        bar5.setInstrumentId("c2209");
        bar5.setMinute(45);
        composite.onBar(bar0);
        composite.onBar(bar1);
        composite.onBar(bar2);
        composite.onBar(bar3);
        composite.onBar(bar4);
        composite.onBar(bar5);
        assertEquals(6, strategy.barCounts().size(), "Bar callback not hit.");
        assertEquals(3, strategy.barCounts().get(bar0).size(), "Bar callback not hit.");
        assertEquals(3, strategy.barCounts().get(bar1).size(), "Bar callback not hit.");
        assertEquals(2, strategy.barCounts().get(bar2).size(), "Bar callback not hit.");
        assertEquals(2, strategy.barCounts().get(bar3).size(), "Bar callback not hit.");
        assertEquals(2, strategy.barCounts().get(bar4).size(), "Bar callback not hit.");
        assertEquals(1, strategy.barCounts().get(bar5).size(), "Bar callback not hit.");
    }

    /**
     * Test of onNotice method, of class AnnotatedStrategy.
     */
    @Test
    public void testOnTradeUpdate() {
        var tradeUpdate = factory.newTradeUpdate();
        composite.onTradeUpdate(tradeUpdate);
        assertEquals(2, strategy.tradeUpdateCounts().size(), "Notice callback not hit.");
    }

}
