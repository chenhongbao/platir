package io.platir.service.api;

import io.platir.service.AdaptorStartupException;

public interface MarketAdaptor {

    /**
     * Allocate resource and connect to remote.
     * @throws io.platir.service.AdaptorStartupException
     */
    void start() throws AdaptorStartupException;

    /**
     * Release resource and close connection to remote.
     */
    void shutdown();

    void add(String instrumentId, MarketListener listener);
}
