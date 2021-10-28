package io.platir.setting;

import java.time.LocalDateTime;

public interface TimeChecker {
    boolean check(LocalDateTime datetime);
    
    boolean hasValue();
}
