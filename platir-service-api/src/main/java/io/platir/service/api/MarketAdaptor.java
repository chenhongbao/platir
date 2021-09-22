package io.platir.service.api;

public interface MarketAdaptor {
	/**
	 * Allocate resource and connect to remote.
	 */
	void initialize();
	
	/**
	 * Release resource and close connection to remote.
	 */
	void shutdown();
	
	void add(String instrumentId, MarketListener listener);
}
