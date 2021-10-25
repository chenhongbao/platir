package io.platir.engine.rule;

import java.time.LocalDateTime;

public abstract class TimeSetter {
    public abstract boolean check(LocalDateTime time);
}
