package io.platir.service;

/**
 *
 * @author Chen Hongbao
 */
public class Constants {

    public static final int CODE_OK = 0;
    public static final int CODE_INVALID_AVAILABLE = 1001;
    public static final int CODE_NO_INSTRUMENT = 1002;
    public static final int CODE_NO_MONEY = 1003;
    public static final int CODE_NO_POSITION = 1004;
    public static final int CODE_RISK_EXCEPTION = 1005;
    public static final int CODE_INVALID_OFFSET = 1006;
    public static final int CODE_RESPONSE_TIMEOUT = 3001;
    public static final int CODE_ORDER_OVER_TRADE = 3002;
    public static final int CODE_NO_LOCK_CONTRACT = 3003;
    public static final int CODE_STRATEGY_EXCEPTION = 4001;
    public static final int CODE_STRATEGY_TIMEOUT = 4002;
    public static final int CODE_TRANSACTION_OVER_TRADE = 4003;
    public static final int CODE_LOGIN_FAIL = 5001;
    public static final int CODE_LOGIN_QUERY_FAIL = 5002;
    public static final int CODE_ORDER_MARKET_CLOSE = 10001;
    public static final int CODE_ORDER_INVALID = 10002;
    public static final int CODE_ORDER_NO_MONEY_POSITION = 10003;
    public static final int CODE_ORDER_INACTIVE_ACCOUNT = 10004;
    public static final String FLAG_CONTRACT_OPEN = "contract-open";
    public static final String FLAG_CONTRACT_OPENING = "contract-opening";
    public static final String FLAG_CONTRACT_CLOSING = "contract-closing";
    public static final String FLAG_CONTRACT_CLOSED = "contract-closed";
    public static final String FLAG_OPEN = "open";
    public static final String FLAG_CLOSE = "close";
    public static final String FLAG_CLOSE_TODAY = "close-today";
    public static final String FLAG_CLOSE_HISTORY = "close-history";
    public static final String FLAG_BUY = "buy";
    public static final String FLAG_SELL = "sell";
    public static final String FLAG_STRATEGY_REMOVED = "strategy-removed";
    public static final String FLAG_STRATEGY_INTERRUPTED = "strategy-interrupted";
    public static final String FLAG_STRATEGY_RUNNING = "strategy-running";
    public static final String FLAG_TRANSACTION_PENDING = "transaction-pending";
    public static final String FLAG_TRANSACTION_EXECUTING = "transaction-executing";
    public static final String FLAG_TRANSACTION_RISK_BLOCKED = "transaction-risk-before";
    public static final String FLAG_TRANSACTION_INVALID = "transaction-invalid";
    public static final String FLAG_TRANSACTION_CHECK_CLOSE = "transaction-check-close";
    public static final String FLAG_TRANSACTION_CHECK_OPEN = "transaction-check-open";
    public static final String FLAG_TRANSACTION_SEND_PENDING = "transaction-send-pending";
    public static final String FLAG_TRANSACTION_SEND_ABORT = "transaction-send-aborted";
    public static final String FLAG_TRANSACTION_SEND_OK = "transaction-send-ok";
}
