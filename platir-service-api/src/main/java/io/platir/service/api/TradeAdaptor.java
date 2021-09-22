package io.platir.service.api;

import io.platir.service.AdaptorStartupException;

public interface TradeAdaptor {

    /**
     * Allocate resource and connect to remote.
     *
     * @throws io.platir.service.AdaptorStartupException
     */
    void start() throws AdaptorStartupException;

    /**
     * Release resource and close connection to remote.
     */
    void shutdown();

    void require(String orderId, String instrumentId, String offset, String direction, Double price, Integer volume,
            TradeListener listener);
}
