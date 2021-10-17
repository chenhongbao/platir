package io.platir.core.internals.persistence.object;

import io.platir.service.Notice;

/**
 *
 * @author Chen Hongbao
 */
class NoticeImpl implements Notice {

    private String message;
    private Integer code;
    private Object object;

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
    public Object getObject() {
        return object;
    }

    @Override
    public void setObject(Object object) {
        this.object = object;
    }

    @Override
    public boolean isGood() {
        return code == 0;
    }
}
