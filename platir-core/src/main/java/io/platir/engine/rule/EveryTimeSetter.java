package io.platir.engine.rule;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

public class EveryTimeSetter extends TimeSetter {

    private final Set<LocalTime> everyTimes = new HashSet<>();
    private final Set<LocalDate> exceptDates = new HashSet<>();
    private final Set<DayOfWeek> exceptDaysOfWeek = new HashSet<>();

    public EveryTimeSetter() {
    }

    public EveryTimeSetter(EveryTimeSetter everyTimeSetter) {
        everyTimes.addAll(everyTimeSetter.getEveryTimes());
        exceptDates.addAll(everyTimeSetter.getExceptDates());
        exceptDaysOfWeek.addAll(everyTimeSetter.getExceptDaysOfWeek());
    }

    @Override
    public boolean check(LocalDateTime time) {
        var alignTime = LocalTime.of(time.getHour(), time.getMinute());
        var today = LocalDate.now();
        return everyTimes.contains(alignTime)
                && !exceptDates.contains(today)
                && !exceptDaysOfWeek.contains(today.getDayOfWeek());
    }

    public Set<LocalTime> getEveryTimes() {
        return new HashSet<>(everyTimes);
    }

    public Set<LocalDate> getExceptDates() {
        return new HashSet<>(exceptDates);
    }

    public Set<DayOfWeek> getExceptDaysOfWeek() {
        return new HashSet<>(exceptDaysOfWeek);
    }

    public EveryTimeSetter every(LocalTime... times) {
        return every(Arrays.asList(times));
    }

    public EveryTimeSetter every(Collection<LocalTime> times) {
        everyTimes.addAll(times.stream()
                .map(time -> LocalTime.of(time.getHour(), time.getMinute()))
                .collect(Collectors.toSet()));
        return this;
    }

    public EveryTimeSetter except(LocalDate... dates) {
        return except(Arrays.asList(dates));
    }

    public EveryTimeSetter except(Collection<LocalDate> dates) {
        exceptDates.addAll(dates);
        return this;
    }

    public EveryTimeSetter except(DayOfWeek... daysOfWeek) {
        exceptDaysOfWeek.addAll(Arrays.asList(daysOfWeek));
        return this;
    }
}
