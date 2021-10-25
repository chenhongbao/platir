package io.platir.broker;

import io.platir.Order;
import java.util.Map;

public interface TradingService {

    void newOrderSingle(ExecutionListener executionListener, Order order);

    void orderCancelRequest(Order order);

    void initialize(Map<String, String> parameters);

    Map<String, String> getParameterHints();
    
    String getVersion();
}