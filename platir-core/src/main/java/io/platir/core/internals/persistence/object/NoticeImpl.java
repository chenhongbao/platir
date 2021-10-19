package io.platir.core.internals.persistence.object;

import io.platir.service.Notice;
import io.platir.service.TransactionContext;

/**
 *
 * @author Chen Hongbao
 */
class NoticeImpl implements Notice {

    private String message;
    private Integer code;
    private Throwable throwable;
    private TransactionContext context;

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
    public TransactionContext getContext() {
        return context;
    }

    @Override
    public void setContext(TransactionContext context) {
        this.context = context;
    }
}
