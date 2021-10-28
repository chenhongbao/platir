package io.platir.setting;

public interface MaxNumberChecker {

    boolean check(Number number);

    Number get();

    void set(Number maximum);

}
