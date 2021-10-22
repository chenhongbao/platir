package io.platir.queries;

import io.platir.service.OrderContext;
import io.platir.service.TransactionContext;
import io.platir.service.TradeUpdate;

/**
 *
 * @author Chen Hongbao
 */
class TradeUpdateImpl implements TradeUpdate {

    private String message;
    private Integer code;
    private Throwable throwable;
    private TransactionContext transactionContext;
    private OrderContext orderContext;

    @Override
    public OrderContext getOrderContext() {
        return orderContext;
    }

    @Override
    public void setOrderContext(OrderContext orderContext) {
        this.orderContext = orderContext;
    }

    @Override
    public String getMessage() {
        return message;
    }

    @Override
    public void setMessage(String message) {
        this.message = message;
    }

    @Override
    public Integer getCode() {
        return code;
    }

    @Override
    public void setCode(Integer code) {
        this.code = code;
    }

    @Override
    public boolean isGood() {
        return code == 0;
    }

    @Override
    public Throwable getError() {
        return throwable;
    }

    @Override
    public void setError(Throwable throwable) {
        this.throwable = throwable;
    }

    @Override
    public TransactionContext getTransactionContext() {
        return transactionContext;
    }

    @Override
    public void setTransactionContext(TransactionContext context) {
        this.transactionContext = context;
    }
}
