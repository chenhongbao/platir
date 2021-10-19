package io.platir.service.api;

import io.platir.service.Trade;

/**
 *
 * @author chenh
 */
public interface TradeListener {

    void onTrade(Trade trade);

    void onNotice(String orderId, int code, String message);
}
