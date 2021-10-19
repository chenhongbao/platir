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

    private final BlockingQueue<NoticeCallbackBundle> noticeBundles = new LinkedBlockingQueue<>();

    void push(Notice notice, OrderExecutionContext context) {
        if (!noticeBundles.offer(new NoticeCallbackBundle(notice, context))) {
            Utils.err.write("Trade callback queue is full.");
        }
    }

    @Override
    public void run() {
        while (!Thread.currentThread().isInterrupted() || !noticeBundles.isEmpty()) {
            try {
                var bundle = noticeBundles.poll(24, TimeUnit.HOURS);
                bundle.executionContext.processNotice(bundle.getNotice().getCode(), bundle.getNotice().getMessage());
            } catch (InterruptedException exception) {
                Utils.err.write("Trade callback queue daemon is interrupted.", exception);
            } catch (Throwable throwable) {
                Utils.err.write("Uncaught error: " + throwable.getMessage(), throwable);
            }
        }
    }

}
