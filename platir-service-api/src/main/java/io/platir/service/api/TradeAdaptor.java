package io.platir.service.api;

public interface TradeAdaptor {

    /**
     * Allocate resource and connect to remote.
     *
     * @throws io.platir.service.api.AdaptorStartupException
     */
    void start() throws AdaptorStartupException;

    /**
     * Release resource and close connection to remote.
     */
    void shutdown();
    
    void setListener(TradeListener listener);

    void require(String orderId, String instrumentId, String offset, String direction, Double price, Integer volume);
}
