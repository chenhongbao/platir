package io.platir.core.internal;

import io.platir.service.Notice;

/**
 *
 * @author Chen Hongbao
 */
class NoticeCallbackBundle {

    final OrderExecutionContext executionContext;
    private final Notice notice;

    NoticeCallbackBundle(Notice notice, OrderExecutionContext executionContext) {
        this.executionContext = executionContext;
        this.notice = notice;
    }

    OrderExecutionContext getExecutionContext() {
        return executionContext;
    }

    Notice getNotice() {
        return notice;
    }

}
