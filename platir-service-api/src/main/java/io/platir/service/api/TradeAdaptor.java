package io.platir.service.api;

public interface TradeAdaptor {

    void start() throws AdaptorStartupException;

    void setListener(TradeListener listener);

    void require(String orderId, String instrumentId, String offset, String direction, Double price, Integer volume);
}
