package io.platir.engine.setting;

import io.platir.engine.PointTimeChecker;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

class PointTimeCheckerCore implements PointTimeChecker {

    private final List<LocalDateTime> times = new LinkedList<>();

    PointTimeCheckerCore() {
    }

    PointTimeCheckerCore(PointTimeChecker pointTimeSetter) {
        times.addAll(pointTimeSetter.getRemainTimes());
    }

    @Override
    public PointTimeCheckerCore at(LocalDateTime... times) {
        return at(Arrays.asList(times));
    }

    @Override
    public PointTimeCheckerCore at(Collection<LocalDateTime> times) {
        this.times.addAll(times.stream()
                .map(time -> LocalDateTime.of(time.getYear(), time.getMonthValue(), time.getDayOfMonth(), time.getHour(), time.getMinute()))
                .collect(Collectors.toSet()));
        this.times.sort((LocalDateTime time1, LocalDateTime time2) -> time1.compareTo(time2));
        return this;
    }

    @Override
    public List<LocalDateTime> getRemainTimes() {
        return new LinkedList<>(times);
    }

    @Override
    public boolean check(LocalDateTime time) {
        var alignTime = LocalDateTime.of(time.getYear(), time.getMonthValue(), time.getDayOfMonth(), time.getHour(), time.getMinute());
        var iterator = this.times.iterator();
        var hit = false;
        while (iterator.hasNext()) {
            var next = iterator.next();
            if (alignTime.compareTo(next) < 0) {
                break;
            } else if (alignTime.compareTo(next) == 0) {
                hit = true;
                iterator.remove();
            } else {
                iterator.remove();
            }
        }
        return hit;
    }
}
