package io.platir.service.api;

import java.util.Map;

public interface MarketAdapter {

    void start() throws AdaptorStartupException;

    Map<String, String> getParamaters();

    void setParameters(Map<String, String> parameters);

    void setListener(MarketListener listener);

    void subscribe(String instrumentId);
}
