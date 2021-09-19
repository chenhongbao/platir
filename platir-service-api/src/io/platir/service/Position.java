package io.platir.service;

public class Position {
	private String instrumentId;
	private String userId;
	private String direction;
	private Integer todayOpenVolume;
	private Integer openVolume;
	private Integer closedVolume;
	private Integer closingVolume;
	private Integer openingVolume;

	public String getInstrumentId() {
		return instrumentId;
	}

	public void setInstrumentId(String instrumentId) {
		this.instrumentId = instrumentId;
	}

	public String getUserId() {
		return userId;
	}

	public void setUserId(String userId) {
		this.userId = userId;
	}

	public String getDirection() {
		return direction;
	}

	public void setDirection(String direction) {
		this.direction = direction;
	}

	public Integer getClosingVolume() {
		return closingVolume;
	}

	public void setClosingVolume(Integer closingVolume) {
		this.closingVolume = closingVolume;
	}

	public Integer getOpeningVolume() {
		return openingVolume;
	}

	public void setOpeningVolume(Integer openingVolume) {
		this.openingVolume = openingVolume;
	}

	public Integer getTodayOpenVolume() {
		return todayOpenVolume;
	}

	public Integer getOpenVolume() {
		return openVolume;
	}

	public Integer getClosedVolume() {
		return closedVolume;
	}

	public void setTodayOpenVolume(Integer todayOpenVolume) {
		this.todayOpenVolume = todayOpenVolume;
	}

	public void setOpenVolume(Integer openVolume) {
		this.openVolume = openVolume;
	}

	public void setClosedVolume(Integer closedVolume) {
		this.closedVolume = closedVolume;
	}

}
