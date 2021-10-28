package io.platir.commons;

import io.platir.setting.PointTimeChecker;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

public class PointTimeCheckerCore implements PointTimeChecker {

    private boolean hasValue = false;
    private final List<LocalDateTime> times = new LinkedList<>();

    public PointTimeCheckerCore() {
    }

    public PointTimeCheckerCore(PointTimeChecker pointTimeSetter) {
        times.addAll(pointTimeSetter.getRemainTimes());
    }

    @Override
    public PointTimeCheckerCore at(LocalDateTime... times) {
        hasValue = true;
        return at(Arrays.asList(times));
    }

    @Override
    public PointTimeCheckerCore at(Collection<LocalDateTime> times) {
        hasValue = true;
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
        if (!hasValue) {
            return false;
        }
        var alignTime = LocalDateTime.of(time.getYear(), time.getMonthValue(), time.getDayOfMonth(), time.getHour(), time.getMinute());
        var iterator = this.times.iterator();
        var hit = false;
        while (iterator.hasNext()) {
            var next = iterator.next();
            if (alignTime.compareTo(next) < 0) {
                break;
            } else if (alignTime.compareTo(next) == 0) {
                hit = true;
                /* Remove obsolete datetime. */
                iterator.remove();
            } else {
                iterator.remove();
            }
        }
        return hit;
    }

    @Override
    public boolean hasValue() {
        return hasValue;
    }
}
