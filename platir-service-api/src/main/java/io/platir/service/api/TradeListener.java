package io.platir.service.api;

import io.platir.service.Trade;

/**
 * Error code explains:
 * <ul>
 * <li>10001: Market is not open.</li>
 * <li>10002: Order is invalid.</li>
 * <li>10003: Broker-side account insufficient money or position.</li>
 * <li>10004: Account is inactive or unavailable.</li>
 * </ul>
 * @author chenh
 */
public interface TradeListener {

    void onTrade(Trade trade);

    void onNotice(String orderId, int code, String message);
}
