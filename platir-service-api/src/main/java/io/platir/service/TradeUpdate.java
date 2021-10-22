package io.platir.service;

public interface TradeUpdate {

    String getMessage();

    void setMessage(String message);

    Integer getCode();

    void setCode(Integer code);

    Throwable getError();

    void setError(Throwable throwable);

    TransactionContext getTransactionContext();

    void setTransactionContext(TransactionContext context);
    
    OrderContext getOrderContext();
    
    void setOrderContext(OrderContext orderContext);

    boolean isGood();

}
