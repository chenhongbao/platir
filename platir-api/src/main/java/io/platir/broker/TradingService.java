package io.platir.broker;

import io.platir.Order;

public interface TradingService {

    void newOrderSingle(ExecutionListener executionListener, Order order);
    
    void orderCancelRequest(Order order);
}
