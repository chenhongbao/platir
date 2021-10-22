package io.platir.service;

public abstract class RiskNotice {

    public static Integer FAIR = 0;
    public static Integer WARNING = 1;
    public static Integer ERROR = 2;
    public static Integer FATAL_ERROR = 3;
    
    public abstract String getRiskNoticeId();
    
    public abstract void setRiskNoticeId(String riskNoticeId);

    public abstract String getStrategyId();

    public abstract String getUserId();

    public abstract Integer getCode();

    public abstract String getMessage();

    public abstract String getUpdateTime();

    public abstract void setStrategyId(String strategyId);

    public abstract void setUserId(String userId);

    public abstract void setCode(Integer code);

    public abstract void setMessage(String message);

    public abstract void setUpdateTime(String updateTime);

    public abstract Integer getLevel();

    public abstract void setLevel(Integer level);

}
