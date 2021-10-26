package io.platir.user;

import io.platir.Transaction;
import io.platir.broker.Bar;
import io.platir.broker.MarketDataSnapshot;

public interface UserStrategy {

    /**
     * Bar update callback.
     * <p>
     * The {@linkplain Bar} is udpated in mixure with
     * {@linkplain MarketDataSnapshot} sequentially in the order of arrivals.
     * The market data is appended to a queue as it arrives and passed to the
     * callback in the order of FIFO. The later data is processed only after its
     * preceeding data is processed and returns. So <b>DO NOT</b> block inside
     * the callback method or it will block all the following market data.
     *
     * @param bar bar update
     */
    void onBar(Bar bar);

    /**
     * Market data snapshot update.
     * <p>
     * Behavior sees {@linkplain #onBar}.
     *
     * @param marketDataSnapshot market data snapshot update
     */
    void onMarketDataSnapshot(MarketDataSnapshot marketDataSnapshot);

    /**
     * Transaction parallel update.
     * <p>
     * {@linkplain Transaction} is updated parallely so the callback needs to
     * handle the concurrecy issue. Please note that there may be two or more
     * callback accessing the same {@linkplain Transaction} instance whose
     * internal state may be changed in calling the method.
     *
     * @param transaction transaction update
     */
    void onTransaction(Transaction transaction);

    /**
     * Callback for the first time {@linkplain UserStrategy} is loaded into
     * system.
     *
     * @param session session for the strategy
     */
    void onLoad(Session session);

    /**
     * Market open callback.
     * <p>
     * The method is called at the moment configured by
     * {@linkplain StrategyRule}.
     */
    void onConfiguredOpen();

    /**
     * Market close callback.
     * <p>
     * The method is called at the moment configured by
     * {@linkplain StrategyRule}.
     */
    void onConfiguredClose();

    /**
     * Alarm callback.
     * <p>
     * The method is called at the moment configured by
     * {@linkplain StrategyRule}.
     */
    void onAlarm();
}
