package io.platir.engine;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

public interface PointTimeChecker extends TimeChecker {

    PointTimeChecker at(LocalDateTime... times);

    PointTimeChecker at(Collection<LocalDateTime> times);

    List<LocalDateTime> getRemainTimes();

}
