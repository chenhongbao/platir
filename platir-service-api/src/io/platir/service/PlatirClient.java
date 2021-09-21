package io.platir.service;

public interface PlatirClient extends PlatirQueryClient {
	TransactionContext open(String instrumentId, String direction, Double price, Integer volume)
			throws TransactionException;

	TransactionContext close(String instrumentId, String direction, Double price, Integer volume)
			throws TransactionException;
}
