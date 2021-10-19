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
    public static final int CODE_ORDER_MONEY_POSITION = 10003;
    public static final int CODE_ORDER_INACTIVE_ACCOUNT =10004;
    public static final String TRANSACTION_STATE_PENDING = "pending";
    public static final String TRANSACTION_STATE_EXECUTING = "executing";
    public static final String TRANSACTION_STATE_RISK_BLOCKED = "risk-before";
    public static final String TRANSACTION_STATE_INVALID = "invalid";
    public static final String TRANSACTION_STATE_CHECK_CLOSE = "check-close";
    public static final String TRANSACTION_STATE_CHECK_OPEN = "check-open";
    public static final String TRANSACTION_STATE_SEND_PENDING = "send-pending";
    public static final String TRANSACTION_STATE_SEND_ABORT = "send-aborted";
    public static final String TRANSACTION_STATE_SEND_OK = "send-ok";
}
