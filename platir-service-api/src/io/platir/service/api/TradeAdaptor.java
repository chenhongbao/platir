package io.platir.service.api;

public interface TradeAdaptor {
	/**
	 * Allocate resource and connect to remote.
	 */
	void initialize();

	/**
	 * Release resource and close connection to remote.
	 */
	void shutdown();

	void require(String orderId, String instrumentId, String offset, String direction, Double price, Integer volume,
			TradeListener listener);
}
