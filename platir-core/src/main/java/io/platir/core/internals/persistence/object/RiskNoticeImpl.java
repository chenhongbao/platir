package io.platir.core.internals.persistence.object;

import io.platir.service.RiskNotice;

/**
 *
 * @author Chen Hongbao
 */
class RiskNoticeImpl extends RiskNotice {

    private String strategyId;
    private String userId;
    private Integer level;
    private Integer code;
    private String message;
    private String updateTime;

    @Override
    public String getStrategyId() {
        return strategyId;
    }

    @Override
    public void setStrategyId(String strategyId) {
        this.strategyId = strategyId;
    }

    @Override
    public String getUserId() {
        return userId;
    }

    @Override
    public void setUserId(String userId) {
        this.userId = userId;
    }

    @Override
    public Integer getLevel() {
        return level;
    }

    @Override
    public void setLevel(Integer level) {
        this.level = level;
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
    public String getMessage() {
        return message;
    }

    @Override
    public void setMessage(String message) {
        this.message = message;
    }

    @Override
    public String getUpdateTime() {
        return updateTime;
    }

    @Override
    public void setUpdateTime(String updateTime) {
        this.updateTime = updateTime;
    }

    @Override
    public boolean isGood() {
        return code == 0;
    }

}
