package io.platir.engine;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collection;
import java.util.Set;

public interface EveryTimeChecker extends TimeChecker {

    EveryTimeChecker every(LocalTime... times);

    EveryTimeChecker every(Collection<LocalTime> times);

    EveryTimeChecker except(LocalDate... dates);

    EveryTimeChecker except(Collection<LocalDate> dates);

    EveryTimeChecker except(DayOfWeek... daysOfWeek);

    Set<LocalTime> getEveryTimes();

    Set<LocalDate> getExceptDates();

    Set<DayOfWeek> getExceptDaysOfWeek();

}
