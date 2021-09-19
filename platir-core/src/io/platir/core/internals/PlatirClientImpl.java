package io.platir.core.internals;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicInteger;

import io.platir.core.PlatirSystem;
import io.platir.service.InvalidTransactionException;
import io.platir.service.PlatirClient;
import io.platir.service.Transaction;
import io.platir.service.TransactionContext;
import io.platir.service.api.Queries;

class PlatirClientImpl extends PlatirQueryImpl implements PlatirClient {

	private final TransactionQueue tr;
	private final AtomicInteger increId = new AtomicInteger(0);
	private final DateTimeFormatter transIdFmt = DateTimeFormatter.ofPattern("yyyyMMdd");

	PlatirClientImpl(StrategyContextImpl strategyContext, TransactionQueue trader, Queries queries) {
		super(strategyContext, queries);
		tr = trader;
	}

	@Override
	public TransactionContext open(String instrumentId, String direction, Double price, Integer volume)
			throws InvalidTransactionException {
		checkTransactionParams(instrumentId, direction, price, volume);
		return push(instrumentId, "open", direction, price, volume);
	}

	private Transaction createTransaction(String strategyId, String instrumentId, String offset, String direction,
			Double price, Integer volume) {
		/*
		 * Don't set state and message here. Only change the values in router daemon.
		 */
		var trans = new Transaction();
		trans.setTransactionId(getTransId());
		trans.setStrategyId(strategyId);
		trans.setInstrumentId(instrumentId);
		trans.setPrice(price);
		trans.setVolume(volume);
		trans.setOffset(offset);
		trans.setDirection(direction);
		trans.setTradingDay(getTradingDay());
		trans.setUpdateTime(PlatirSystem.datetime());
		return trans;
	}

	private String getTransId() {
		/*
		 * yyyyMMdd + <4-digits> 200808120012
		 */
		return LocalDate.now().format(transIdFmt) + String.format("%4d", increId.incrementAndGet());
	}

	private void checkTransactionParams(String instrumentId, String direction, Double price, Integer volume)
			throws InvalidTransactionException {
		if (instrumentId == null || instrumentId.isBlank()) {
			throw new InvalidTransactionException("Invalid instrument ID(\"" + instrumentId + "\").");
		}
		if (direction == null
				|| (direction.compareToIgnoreCase("buy") != 0 && direction.compareToIgnoreCase("sell") != 0)) {
			throw new InvalidTransactionException("Invalid direction(\"" + direction + "\").");
		}
		if (volume <= 0) {
			throw new InvalidTransactionException("Invalid volume(" + volume + ").");
		}
		if (price <= 0) {
			throw new InvalidTransactionException("Invalid price(" + price + ").");
		}
	}

	@Override
	public TransactionContext close(String instrumentId, String direction, Double price, Integer volume)
			throws InvalidTransactionException {
		checkTransactionParams(instrumentId, direction, price, volume);
		return push(instrumentId, "close", direction, price, volume);
	}

	private TransactionContext push(String instrumentId, String offset, String direction, Double price,
			Integer volume) {
		var trans = createTransaction(getStrategyId(), instrumentId, offset, direction, price, volume);
		var transCtx = new TransactionContextImpl(trans, getStrategyContext());
		/* strategy context has the transaction context. */
		getStrategyContext().addTransactionContext(transCtx);
		/* save transaction to data source */
		getStrategyContext().getPlatirClientImpl().add(trans);
		/* send the order and update trades into TransactionContext. */
		tr.push(transCtx);
		return transCtx;
	}

}
