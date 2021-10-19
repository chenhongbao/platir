package io.platir.service;

import java.net.URI;

public interface StrategyProfile {

    String getCreateDate();

    String getRemoveDate();

    void setCreateDate(String createDate);

    void setRemoveDate(String removeDate);

    String getUserId();

    void setUserId(String userId);

    String[] getArgs();

    void setArgs(String[] args);

    String getPassword();

    String[] getInstrumentIds();

    void setPassword(String password);

    void setInstrumentIds(String[] instrumentIds);

    String getStrategyId();

    void setStrategyId(String strategyId);

    String getState();

    void setState(String state);

    URI getUri();

    void setUri(URI uri);
}
