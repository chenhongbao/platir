package io.platir.engine.rule;

import java.util.concurrent.atomic.AtomicReference;

public class MaxNumberRule {
    private final AtomicReference<Number> max  = new AtomicReference();
    
    public MaxNumberRule(Number defaultValue) {
        max.set(defaultValue);
    }
    
    public boolean check(Number number) {
        return max.get().doubleValue() < number.doubleValue();
    }
    
    public void set(Number maximum) {
        max.set(maximum);
    }
    
    public Number get() {
        return max.get();
    }
}
