package io.platir.broker;

import io.platir.Order;
import java.util.Map;

public interface TradingService {

    /**
     * Submit a new order request to broker and return code indicates whether
     * the order is accepted by the broker.
     * <p>
     * The method call the specified {@linkplain ExecutionListener} for
     * execution report updated until the order is totally filled, canceled or
     * rejected. There can be a single {@linkplain ExecutionListener} for
     * multiple orders, but it does not ganrantee a sequential callback. So the
     * specified {@linkplain ExecutionListener} must handle the possible
     * multi-threading issue.
     * <p>
     * If the broker returns acceptance feedback via an asynchronized callback,
     * the implementation shall block until it has the feedback. The method
     * guarantees throwing no exception but returning error codes. Returning
     * <b>Zero</b>/{@code 0} indicates a successful acceptance of the order by
     * broker.
     *
     * @param order order to submit to broker
     * @param executionListener execution listener for report callback
     * @return {@code 0} if submitted order has been accepted by broker, error
     * otherwise.
     */
    int newOrderSingle(Order order, ExecutionListener executionListener);

    /**
     * Submit a cancel request for the specified order and return code indicates
     * whether the request has been accepted by broker.
     * <p>
     * If the specified order has been totally filled, canceled or rejected, or
     * in any state except queueing, the request shall fail and order state is
     * not changed, and the associated {@linkplain ExecutionListener} shall
     * receive no feedback. But if the order is successfully canceled,
     * {@linkplain ExecutionListener} shall have the feedback.
     * <p>
     * For a better notification upon request failure, it is recommended that
     * implementation blocks until it has the feedback for the request then
     * returns the error code from the method. Any return code other than
     * <b>Zero</b>/{@code 0} indicates cancel request failure.
     *
     *
     * @param order order to cancel by broker
     * @return {@code 0} if cancel request is accepted by broker, error
     * otherwise
     */
    int orderCancelRequest(Order order);

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
    String getVersion();
}
