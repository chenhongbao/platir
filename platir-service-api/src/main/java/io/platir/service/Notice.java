package io.platir.service;

public interface Notice {

    String getMessage();

    void setMessage(String message);

    Integer getCode();

    void setCode(Integer code);

    Throwable getError();

    void setError(Throwable throwable);

    TransactionContext getContext();

    void setContext(TransactionContext context);

    boolean isGood();

}
