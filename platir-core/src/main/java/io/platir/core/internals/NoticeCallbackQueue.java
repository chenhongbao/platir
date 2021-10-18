package io.platir.core.internals;

import io.platir.service.Notice;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 *
 * @author Chen Hongbao
 */
class NoticeCallbackQueue implements Runnable {

    private final BlockingQueue<NoticeCallbackBundle> bundles = new LinkedBlockingQueue<>();

    void push(Notice notice, OrderExecutionContext context) {
        if (!bundles.offer(new NoticeCallbackBundle(notice, context))) {
            Utils.err.write("Trade callback queue is full.");
        }
    }

    @Override
    public void run() {
        while (!Thread.currentThread().isInterrupted() || !bundles.isEmpty()) {
            try {
                io.platir.core.internals.NoticeCallbackQueue.NoticeCallbackBundle b = bundles.poll(24, TimeUnit.HOURS);
                b.ctx.processNotice(b.nt.getCode(), b.nt.getMessage());
            } catch (InterruptedException ex) {
                Utils.err.write("Trade callback queue daemon is interrupted.", ex);
            } catch (Throwable th) {
                Utils.err.write("Uncaught error: " + th.getMessage(), th);
            }
        }
    }

    private class NoticeCallbackBundle {

        private final OrderExecutionContext ctx;
        private final Notice nt;

        public NoticeCallbackBundle(Notice notice, OrderExecutionContext context) {
            ctx = context;
            nt = notice;
        }
    }

}
