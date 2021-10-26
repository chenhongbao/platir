package io.platir.user;

import io.platir.Account;
import io.platir.Instrument;
import io.platir.Transaction;
import java.util.Map;
import java.util.logging.Logger;

public interface Session {

    Transaction buyOpen(String instrumentId, String exchangeId, Double price, Integer quantity) throws NewOrderException;

    Transaction sellOpen(String instrumentId, String exchangeId, Double price, Integer quantity) throws NewOrderException;

    Transaction buyCloseToday(String instrumentId, String exchangeId, Double price, Integer quantity) throws NewOrderException;

    Transaction buyCloseYesterday(String instrumentId, String exchangeId, Double price, Integer quantity) throws NewOrderException;

    Transaction sellCloseToday(String instrumentId, String exchangeId, Double price, Integer quantity) throws NewOrderException;

    Transaction sellCloseYesterday(String instrumentId, String exchangeId, Double price, Integer quantity) throws NewOrderException;

    void cancel(Transaction transaction) throws CancelOrderException;

    void marketDataRequest(String instrumentId) throws MarketDataRequestException;

    Instrument getInstrument(String instrumentId);

    Account getAccount();

    Logger getLogger();

    Map<String, String> getParameters();
}
