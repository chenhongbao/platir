package io.platir.service;

public class StrategyProfile {
	private String strategyId;
	private String userId;
	private String password;
	private String state;
	private String createDate;
	private String removeDate;
	private String[] instrumentIds;
	private String[] args;

	public String getCreateDate() {
		return createDate;
	}

	public String getRemoveDate() {
		return removeDate;
	}

	public void setCreateDate(String createDate) {
		this.createDate = createDate;
	}

	public void setRemoveDate(String removeDate) {
		this.removeDate = removeDate;
	}

	public String getUserId() {
		return userId;
	}

	public void setUserId(String userId) {
		this.userId = userId;
	}

	public String[] getArgs() {
		return args;
	}

	public void setArgs(String[] args) {
		this.args = args;
	}

	public String getPassword() {
		return password;
	}

	public String[] getInstrumentIds() {
		return instrumentIds;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public void setInstrumentIds(String[] instrumentIds) {
		this.instrumentIds = instrumentIds;
	}

	public String getStrategyId() {
		return strategyId;
	}

	public void setStrategyId(String strategyId) {
		this.strategyId = strategyId;
	}

	public String getState() {
		return state;
	}

	public void setState(String state) {
		this.state = state;
	}

}
