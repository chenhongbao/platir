package io.platir.core.internal;

import io.platir.service.Bar;
import io.platir.service.Notice;
import io.platir.service.PlatirClient;
import io.platir.service.Tick;
import io.platir.service.Trade;
import io.platir.service.annotation.OnBar;
import io.platir.service.annotation.OnDestroy;
import io.platir.service.annotation.OnNotice;
import io.platir.service.annotation.OnStart;
import io.platir.service.annotation.OnStop;
import io.platir.service.annotation.OnTick;
import io.platir.service.annotation.OnTrade;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 *
 * @author Chen Hongbao
 */
public class AnnotationStrategy {

    private final Map<Bar, List<String>> barCounts = new HashMap<>();
    private final Map<Tick, List<String>> tickCounts = new HashMap<>();
    private final Map<Trade, List<String>> tradeCounts = new HashMap<>();
    private final List<String> destroyCounts = new LinkedList<>();
    private final List<String> noticeCounts = new LinkedList<>();
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

    List<String> noticeCounts() {
        return noticeCounts;
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

    @OnNotice
    public void whenNotice(Notice notice) {
        noticeCounts.add("@OnNotice");
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
