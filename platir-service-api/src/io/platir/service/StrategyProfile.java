package io.platir.service;

public class StrategyProfile {
	private String strategyId;
	private String userId;
	private String password;
	private String[] instrumentIds;
	private String[] args;

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

}
