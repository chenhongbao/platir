package io.platir.service;

public interface PlatirClient extends PlatirQuery {
	TransactionContext open(String instrumentId, String direction, Double price, Integer volume)
			throws InvalidTransactionException;

	TransactionContext close(String instrumentId, String direction, Double price, Integer volume)
			throws InvalidTransactionException;
}
