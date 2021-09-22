package io.platir.service;

public class Instrument {
	private String instrumentId;
	private String exchangeId;
	private Double multiple;
	private Double amountMargin;
	private Double volumeMargin;
	private Double amountCommission;
	private Double volumeCommission;
	private String updateTime;

	public String getInstrumentId() {
		return instrumentId;
	}

	public void setInstrumentId(String instrumentId) {
		this.instrumentId = instrumentId;
	}

	public String getExchangeId() {
		return exchangeId;
	}

	public void setExchangeId(String exchangeId) {
		this.exchangeId = exchangeId;
	}

	public Double getMultiple() {
		return multiple;
	}

	public void setMultiple(Double multiple) {
		this.multiple = multiple;
	}

	public Double getAmountMargin() {
		return amountMargin;
	}

	public void setAmountMargin(Double amountMargin) {
		this.amountMargin = amountMargin;
	}

	public Double getVolumeMargin() {
		return volumeMargin;
	}

	public void setVolumeMargin(Double volumeMargin) {
		this.volumeMargin = volumeMargin;
	}

	public Double getAmountCommission() {
		return amountCommission;
	}

	public void setAmountCommission(Double amountCommission) {
		this.amountCommission = amountCommission;
	}

	public Double getVolumeCommission() {
		return volumeCommission;
	}

	public void setVolumeCommission(Double volumeCommission) {
		this.volumeCommission = volumeCommission;
	}

	public String getUpdateTime() {
		return updateTime;
	}

	public void setUpdateTime(String updateTime) {
		this.updateTime = updateTime;
	}

}
