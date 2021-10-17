package io.platir.service;

public interface Position {

    String getInstrumentId();

    void setInstrumentId(String instrumentId);

    String getUserId();

    void setUserId(String userId);

    String getDirection();

    void setDirection(String direction);

    Integer getClosingVolume();

    void setClosingVolume(Integer closingVolume);

    Integer getOpeningVolume();

    void setOpeningVolume(Integer openingVolume);

    Integer getTodayOpenVolume();

    Integer getOpenVolume();

    Integer getClosedVolume();

    void setTodayOpenVolume(Integer todayOpenVolume);

    void setOpenVolume(Integer openVolume);

    void setClosedVolume(Integer closedVolume);

}
