package io.platir.core.internal;

import io.platir.core.AnnotationParsingException;
import io.platir.queries.FactoryImpl;
import io.platir.service.Bar;
import io.platir.service.Factory;
import io.platir.service.PlatirClient;
import io.platir.service.Strategy;
import io.platir.service.Tick;
import io.platir.service.Trade;
import io.platir.service.TradeUpdate;
import io.platir.service.annotation.OnBar;
import io.platir.service.annotation.OnDestroy;
import io.platir.service.annotation.OnStart;
import io.platir.service.annotation.OnStop;
import io.platir.service.annotation.OnTick;
import io.platir.service.annotation.OnTrade;
import io.platir.service.annotation.OnTradeUpdate;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
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

    private static class AnnotationStrategy {

        private final Map<Bar, List<String>> barCounts = new HashMap<>();
        private final Map<Tick, List<String>> tickCounts = new HashMap<>();
        private final Map<Trade, List<String>> tradeCounts = new HashMap<>();
        private final List<String> destroyCounts = new LinkedList<>();
        private final List<String> tradeUpdateCounts = new LinkedList<>();
        private final List<String> startCounts = new LinkedList<>();
        private final List<String> stopCounts = new LinkedList<>();

        Map<Bar, List<String>> barCounts() {
            return new HashMap<>(barCounts);
        }

        Map<Tick, List<String>> tickCounts() {
            return new HashMap<>(tickCounts);
        }

        Map<Trade, List<String>> tradeCounts() {
            return new HashMap<>(tradeCounts);
        }

        List<String> destroyCounts() {
            return destroyCounts;
        }

        List<String> tradeUpdateCounts() {
            return tradeUpdateCounts;
        }

        List<String> startCounts() {
            return startCounts;
        }

        List<String> stopCounts() {
            return stopCounts;
        }

        void addBarCount(Bar bar, String method) {
            var list = barCounts.computeIfAbsent(bar, key -> new LinkedList<String>());
            list.add(method);
        }

        @OnBar(id = {"c2201", "rb2210"})
        public void whenBar(Bar bar) {
            addBarCount(bar, "@OnBar(id = {\"c2201\", \"br2210\"})");
        }

        @OnBar(id = "rb2210", minute = {1, 5})
        public void whenBarRB2210(Bar bar) {
            addBarCount(bar, "OnBar(id = \"rb2210\", minute = {1, 5})");
        }

        @OnDestroy
        public void whenDestroy() {
            destroyCounts.add("@OnDestroy");
        }

        @OnTradeUpdate
        public void whenTradeUpdate(TradeUpdate update) {
            tradeUpdateCounts.add("@OnTradeUpdate");
        }

        @OnStart
        public void whenStart(String[] args, PlatirClient client) {
            startCounts.add("@OnStart");
        }

        @OnStop
        public void whenStop(int reason) {
            stopCounts.add("@OnStop");
        }

        void addTickCount(Tick tick, String method) {
            var list = tickCounts.computeIfAbsent(tick, key -> new LinkedList<String>());
            list.add(method);
        }

        @OnTick(id = {"c2205", "c2209"})
        public void whenTickC(Tick tick) {
            addTickCount(tick, "@OnTick(id = {\"c2205\", \"c2201\"})");
        }

        @OnTick(id = "rb2210")
        public void whenTickRB2210(Tick tick) {
            addTickCount(tick, "@OnTick(id = \"rb2210\")");
        }

        void addTradeCount(Trade trade, String method) {
            var list = tradeCounts.computeIfAbsent(trade, key -> new LinkedList<String>());
            list.add(method);
        }

        @OnTrade
        public void whenTrade(Trade trade) {
            addTradeCount(trade, "@OnTrade");
        }
    }

    private static class SuperStrategy extends AnnotationStrategy implements Strategy {

        @Override
        public void onStart(String[] args, PlatirClient platir) {
            startCounts().add("onStart()");
        }

        @Override
        public void onStop(int reason) {
            stopCounts().add("onStop()");
        }

        @Override
        public void onDestroy() {
            destroyCounts().add("onDestroy()");
        }

        @Override
        public void onTrade(Trade trade) {
            addTradeCount(trade, "onTrade()");
        }

        @Override
        public void onTick(Tick tick) {
            addTickCount(tick, "onTick()");
        }

        @Override
        public void onBar(Bar bar) {
            addBarCount(bar, "onBar()");
        }

        @Override
        public void onTradeUpdate(TradeUpdate notice) {
            tradeUpdateCounts().add("onNotice()");
        }

    }
}
