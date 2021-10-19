package io.platir.core.internal.objects;

import io.platir.service.Position;

/**
 *
 * @author Chen Hongbao
 */
class PositionImpl implements Position {

    private String instrumentId;
    private String userId;
    private String direction;
    private Integer todayOpenVolume;
    private Integer openVolume;
    private Integer closedVolume;
    private Integer closingVolume;
    private Integer openingVolume;

    @Override
    public String getInstrumentId() {
        return instrumentId;
    }

    @Override
    public void setInstrumentId(String instrumentId) {
        this.instrumentId = instrumentId;
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
    public String getDirection() {
        return direction;
    }

    @Override
    public void setDirection(String direction) {
        this.direction = direction;
    }

    @Override
    public Integer getTodayOpenVolume() {
        return todayOpenVolume;
    }

    @Override
    public void setTodayOpenVolume(Integer todayOpenVolume) {
        this.todayOpenVolume = todayOpenVolume;
    }

    @Override
    public Integer getOpenVolume() {
        return openVolume;
    }

    @Override
    public void setOpenVolume(Integer openVolume) {
        this.openVolume = openVolume;
    }

    @Override
    public Integer getClosedVolume() {
        return closedVolume;
    }

    @Override
    public void setClosedVolume(Integer closedVolume) {
        this.closedVolume = closedVolume;
    }

    @Override
    public Integer getClosingVolume() {
        return closingVolume;
    }

    @Override
    public void setClosingVolume(Integer closingVolume) {
        this.closingVolume = closingVolume;
    }

    @Override
    public Integer getOpeningVolume() {
        return openingVolume;
    }

    @Override
    public void setOpeningVolume(Integer openingVolume) {
        this.openingVolume = openingVolume;
    }

}
