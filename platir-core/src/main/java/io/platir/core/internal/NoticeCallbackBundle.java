package io.platir.core.internal;

import io.platir.service.TradeUpdate;

/**
 *
 * @author Chen Hongbao
 */
class NoticeCallbackBundle {

    final OrderExecutionContext executionContext;
    private final TradeUpdate notice;

    NoticeCallbackBundle(TradeUpdate notice, OrderExecutionContext executionContext) {
        this.executionContext = executionContext;
        this.notice = notice;
    }

    OrderExecutionContext getExecutionContext() {
        return executionContext;
    }

    TradeUpdate getNotice() {
        return notice;
    }

}
