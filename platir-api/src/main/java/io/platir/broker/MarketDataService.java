package io.platir.broker;

import java.util.Map;

public interface MarketDataService {

    /**
     * Make market data request to broker for the specified instrument and
     * receive market data reponse and updates via the sepcified
     * {@linkplain MarketDataListener}.
     * <p>
     * If request is accepted by broker, the method return <b>zero</b>/{@code 0}
     * and
     * {@linkplain MarketDataListener#onMarketDataResponse onMarketDataResponse}
     * is called with hints about the upcoming market data types. If broker
     * rejects the requests, the method return error code and no feedback or
     * market data updates for {@linkplain MarketDataListener}.
     * <p>
     * Usually the market data for the specified instrument is updated via the
     * specified listener and different instrument can hvae different listeners.
     * But there can be multiple instruments' market data updated via one single
     * listener, and it must handle that multi-threading issue.
     * <p>
     * If the broker returns request feedback via an asynchronzed callback, the
     * implementation shall block until it has information about acceptence of
     * the request by broker. And then return error codes or <b>zero</b> from
     * the method. The method ganrantees throwing no exception.
     *
     * @param instrumentId instrument to request for market data
     * @param listener listener for request response and possible market data
     * updates
     * @return {@code 0} if request is accepted by broker, error codes
     * otherwise.
     */
    int marketDataRequest(String instrumentId, MarketDataListener listener);

    /**
     * Set parameters for service initialization.
     * <p>
     * Usually the parameters contain information for authentication and login
     * to broker, and configuration for reconnect or holidays.
     *
     * @param parameters intialization parameters
     * @return {@code 0} if the service is intialized successfully, error
     * otherwise
     */
    int initialize(Map<String, String> parameters);

    /**
     * Return a parameter mapping as a hint for parameter settings.
     * <p>
     * If the parameters are not set yet, the values in mapping are empty
     * strings. If parameters have been set, the mapping shall have values.
     *
     * @return parameters' key-value mapping whose values may be empty.
     */
    Map<String, String> getParameterHints();

    /**
     * Get service version providing information about the underlying
     * implementation.
     *
     * @return version string.
     */
    String getServiceVersion();

    /**
     * Get service name providing information about the underlying
     * implementation.
     *
     * @return name string
     */
    String getServiceName();
}
