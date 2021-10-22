package io.platir.service;

/**
 *
 * @author Chen Hongbao
 */
public class ServiceConstants {

    public static final int CODE_OK = 0;
    public static final int CODE_TRANSACTION_COMPLETED = 1;
    public static final int CODE_INVALID_AVAILABLE = 1001;
    public static final int CODE_NO_INSTRUMENT = 1002;
    public static final int CODE_NO_MONEY = 1003;
    public static final int CODE_NO_POSITION = 1004;
    public static final int CODE_INVALID_OFFSET = 1005;
    public static final int CODE_TRANSACTION_SEND_OK = 2001;
    public static final int CODE_TRANSACTION_SEND_PENDING = 2002;
    public static final int CODE_TRANSACTION_SEND_PART = 2003;
    public static final int CODE_TRANSACTION_SEND_ABORT = 2004;
    public static final int CODE_RESPONSE_TIMEOUT = 3001;
    public static final int CODE_ORDER_OVER_TRADE = 3002;
    public static final int CODE_NO_LOCK_CONTRACT = 3003;
    public static final int CODE_STRATEGY_EXCEPTION = 4001;
    public static final int CODE_TRANSACTION_OVER_TRADE = 4002;
    public static final int CODE_LOGIN_FAIL = 5001;
    public static final int CODE_LOGIN_QUERY_FAIL = 5002;
    public static final String FLAG_CONTRACT_OPEN = "contract-open";
    public static final String FLAG_CONTRACT_OPENING = "contract-opening";
    public static final String FLAG_CONTRACT_CLOSING = "contract-closing";
    public static final String FLAG_CONTRACT_CLOSED = "contract-closed";
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
    public static final String FLAG_TRANSACTION_SEND_PART = "transaction-send-part";
    public static final String FLAG_TRANSACTION_SEND_OK = "transaction-send-ok";
}
