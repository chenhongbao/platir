package io.platir.service;

public class RiskNotice {

	public static Integer FAIR = 0;
	public static Integer WARNING = 1;
	public static Integer ERROR = 2;
	public static Integer FATAL_ERROR = 3;

	private String strategyId;
	private String userId;
	private Integer level;
	private Integer code;
	private String message;
	private String updateTime;

	public String getStrategyId() {
		return strategyId;
	}

	public String getUserId() {
		return userId;
	}

	public Integer getCode() {
		return code;
	}

	public String getMessage() {
		return message;
	}

	public String getUpdateTime() {
		return updateTime;
	}

	public void setStrategyId(String strategyId) {
		this.strategyId = strategyId;
	}

	public void setUserId(String userId) {
		this.userId = userId;
	}

	public void setCode(Integer code) {
		this.code = code;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public void setUpdateTime(String updateTime) {
		this.updateTime = updateTime;
	}

	public Integer getLevel() {
		return level;
	}

	public void setLevel(Integer level) {
		this.level = level;
	}
	
	public boolean isGood() {
		return code != null && code == 0;
	}

}
