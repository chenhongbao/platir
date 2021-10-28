package io.platir.commons;

import io.platir.setting.EveryTimeChecker;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

public class EveryTimeCheckerCore implements EveryTimeChecker {

    private final Set<LocalTime> everyTimes = new HashSet<>();
    private final Set<LocalDate> exceptDates = new HashSet<>();
    private final Set<DayOfWeek> exceptDaysOfWeek = new HashSet<>();

    public EveryTimeCheckerCore() {
    }

    public EveryTimeCheckerCore(EveryTimeChecker everyTimeChecker) {
        everyTimes.addAll(everyTimeChecker.getEveryTimes());
        exceptDates.addAll(everyTimeChecker.getExceptDates());
        exceptDaysOfWeek.addAll(everyTimeChecker.getExceptDaysOfWeek());
    }

    @Override
    public boolean check(LocalDateTime datetime) {
        var alignTime = LocalTime.of(datetime.getHour(), datetime.getMinute());
        var date = datetime.toLocalDate();
        return everyTimes.contains(alignTime) && !exceptDates.contains(date) && !exceptDaysOfWeek.contains(date.getDayOfWeek());
    }

    @Override
    public Set<LocalTime> getEveryTimes() {
        return new HashSet<>(everyTimes);
    }

    @Override
    public Set<LocalDate> getExceptDates() {
        return new HashSet<>(exceptDates);
    }

    @Override
    public Set<DayOfWeek> getExceptDaysOfWeek() {
        return new HashSet<>(exceptDaysOfWeek);
    }

    @Override
    public EveryTimeCheckerCore every(LocalTime... times) {
        return every(Arrays.asList(times));
    }

    @Override
    public EveryTimeCheckerCore every(Collection<LocalTime> times) {
        everyTimes.addAll(times.stream()
                .map(time -> LocalTime.of(time.getHour(), time.getMinute()))
                .collect(Collectors.toSet()));
        return this;
    }

    @Override
    public EveryTimeCheckerCore except(LocalDate... dates) {
        return except(Arrays.asList(dates));
    }

    @Override
    public EveryTimeCheckerCore except(Collection<LocalDate> dates) {
        exceptDates.addAll(dates);
        return this;
    }

    @Override
    public EveryTimeCheckerCore except(DayOfWeek... daysOfWeek) {
        exceptDaysOfWeek.addAll(Arrays.asList(daysOfWeek));
        return this;
    }
}
