package io.platir.engine.timer;

import java.util.Calendar;
import java.util.Date;
import java.util.Timer;
import java.util.concurrent.TimeUnit;

class EngineTimerCore extends EngineTimer {

    private final Timer timer;
    private final TimerJobScheduler jobScheduler;

    EngineTimerCore() {
        jobScheduler = new TimerJobScheduler(this);
        timer = new Timer();
        timer.scheduleAtFixedRate(jobScheduler, getNextMinute(), TimeUnit.MINUTES.toMillis(1));
    }

    @Override
    public void addJob(TimerJob job) {
        jobScheduler.addJob(job);
    }

    @Override
    public void removeJob(TimerJob job) {
        jobScheduler.removeJob(job);
    }

    private Date getNextMinute() {
        var calendar = Calendar.getInstance();
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        calendar.roll(Calendar.MINUTE, 1);
        return calendar.getTime();
    }

}
