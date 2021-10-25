package io.platir.user;

public class MarketDataRequestException extends Exception {

    public MarketDataRequestException(String message) {
        super(message);
    }

    public MarketDataRequestException(String message, Throwable cause) {
        super(message, cause);
    }

}
