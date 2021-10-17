package io.platir.service;

public interface Notice {

    String getMessage();

    void setMessage(String message);

    Integer getCode();

    void setCode(Integer code);

    Object getObject();

    void setObject(Object object);

    boolean isGood();

}
