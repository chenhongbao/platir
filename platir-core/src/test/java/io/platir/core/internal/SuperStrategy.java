package io.platir.core.internal;

import io.platir.service.Bar;
import io.platir.service.Notice;
import io.platir.service.PlatirClient;
import io.platir.service.Strategy;
import io.platir.service.Tick;
import io.platir.service.Trade;

/**
 *
 * @author Chen Hongbao
 */
class SuperStrategy extends AnnotationStrategy implements Strategy {

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
    public void onNotice(Notice notice) {
        noticeCounts().add("onNotice()");
    }

}
