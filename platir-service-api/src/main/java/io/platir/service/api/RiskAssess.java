package io.platir.service.api;

import io.platir.service.OrderContext;
import io.platir.service.RiskNotice;
import io.platir.service.Tick;
import io.platir.service.Trade;
import io.platir.service.TransactionContext;

public interface RiskAssess {
	RiskNotice before(Tick current, TransactionContext transaction);

	RiskNotice after(Trade trade, TransactionContext transaction);

	/**
	 * Receive non-risk management, but fatal error.
	 */
	void notice(int code, String message, OrderContext order);

	/**
	 * Receive non-risk management, but fatal error.
	 */
	void notice(int code, String message);
}