package io.platir.service.api;

import java.util.Map;

public interface TradeAdapter {

    int start();

    int require(String orderId, String instrumentId, String offset, String direction, Double price, Integer volume);

    Map<String, String> getParamaters();

    void setParameters(Map<String, String> parameters);

    void setListener(TradeListener listener);
}
