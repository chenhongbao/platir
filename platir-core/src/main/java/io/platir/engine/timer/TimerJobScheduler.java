package io.platir.engine.timer;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentSkipListSet;

class TimerJobScheduler extends TimerTask {

    private final Set<TimerJob> timerJobs = new ConcurrentSkipListSet<>();
    private final EngineTimer timer;

    TimerJobScheduler(EngineTimer timer) {
        this.timer = timer;
    }

    @Override
    public void run() {
        final var now = LocalDateTime.now();
        timerJobs.parallelStream().forEach(job -> {
            var alignDatetime = LocalDateTime.of(now.getDayOfYear(), now.getMonthValue(), now.getDayOfMonth(), now.getHour(), now.getMinute());
            try {
                if (now.getSecond() >= 30) {
                    job.onTime(alignDatetime.plusMinutes(1), timer);
                } else {
                    job.onTime(alignDatetime, timer);
                }
            } catch (Throwable throwable) {
            }
        });
    }

    void addJob(TimerJob job) {
        timerJobs.add(job);
    }

    void removeJob(TimerJob job) {
        timerJobs.remove(job);
    }
}
