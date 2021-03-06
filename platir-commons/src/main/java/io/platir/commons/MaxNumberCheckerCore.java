package io.platir.commons;

import java.util.concurrent.atomic.AtomicReference;
import io.platir.setting.MaxNumberChecker;

public class MaxNumberCheckerCore implements MaxNumberChecker {

    private final AtomicReference<Number> max = new AtomicReference();

    public MaxNumberCheckerCore(Number defaultValue) {
        max.set(defaultValue);
    }

    @Override
    public boolean check(Number number) {
        return max.get().doubleValue() < number.doubleValue();
    }

    @Override
    public void set(Number maximum) {
        max.set(maximum);
    }

    @Override
    public Number get() {
        return max.get();
    }
}
