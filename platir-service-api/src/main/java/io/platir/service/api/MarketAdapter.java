package io.platir.service.api;

import java.util.Map;

public interface MarketAdapter {

    int start();

    int subscribe(String instrumentId);

    Map<String, String> getParamaters();

    void setParameters(Map<String, String> parameters);

    void setListener(MarketListener listener);
}
