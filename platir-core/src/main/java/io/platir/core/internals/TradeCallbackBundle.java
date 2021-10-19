package io.platir.core.internals;

import io.platir.service.Trade;

/**
 *
 * @author Chen Hongbao
 */
class TradeCallbackBundle {

    final OrderExecutionContext executionContext;
    final Trade trade;

    public TradeCallbackBundle(Trade trade, OrderExecutionContext executionContext) {
        this.executionContext = executionContext;
        this.trade = trade;
    }

    OrderExecutionContext getExecutionContext() {
        return executionContext;
    }

    Trade getTrade() {
        return trade;
    }

}
