package io.platir.service;

public class Account {
	private String accountId;
	private String userId;
	private Double balance;
	private Double margin;
	private Double commission;
	private Double openingMargin;
	private Double openingCommission;
	private Double closingCommission;
	private Double available;
	private Double positionProfit;
	private Double closeProfit;
	private Double ydBalance;
	private String tradingDay;
	private String settleTime;

	public String getAccountId() {
		return accountId;
	}

	public String getUserId() {
		return userId;
	}

	public void setAccountId(String accountId) {
		this.accountId = accountId;
	}

	public void setUserId(String userId) {
		this.userId = userId;
	}

	public Double getOpeningMargin() {
		return openingMargin;
	}

	public void setOpeningMargin(Double openingMargin) {
		this.openingMargin = openingMargin;
	}

	public Double getOpeningCommission() {
		return openingCommission;
	}

	public void setOpeningCommission(Double openingCommission) {
		this.openingCommission = openingCommission;
	}

	public Double getClosingCommission() {
		return closingCommission;
	}

	public void setClosingCommission(Double closingCommission) {
		this.closingCommission = closingCommission;
	}

	public Double getBalance() {
		return balance;
	}

	public void setBalance(Double balance) {
		this.balance = balance;
	}

	public Double getMargin() {
		return margin;
	}

	public void setMargin(Double margin) {
		this.margin = margin;
	}

	public Double getCommission() {
		return commission;
	}

	public void setCommission(Double commission) {
		this.commission = commission;
	}

	public Double getAvailable() {
		return available;
	}

	public void setAvailable(Double available) {
		this.available = available;
	}

	public Double getPositionProfit() {
		return positionProfit;
	}

	public void setPositionProfit(Double positionProfit) {
		this.positionProfit = positionProfit;
	}

	public Double getCloseProfit() {
		return closeProfit;
	}

	public void setCloseProfit(Double closeProfit) {
		this.closeProfit = closeProfit;
	}

	public Double getYdBalance() {
		return ydBalance;
	}

	public void setYdBalance(Double ydBalance) {
		this.ydBalance = ydBalance;
	}

	public String getTradingDay() {
		return tradingDay;
	}

	public void setTradingDay(String tradingDay) {
		this.tradingDay = tradingDay;
	}

	public String getSettleTime() {
		return settleTime;
	}

	public void setSettleTime(String settleTime) {
		this.settleTime = settleTime;
	}

}
