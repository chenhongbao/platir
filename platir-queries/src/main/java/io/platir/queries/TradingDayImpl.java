package io.platir.queries;

import io.platir.service.TradingDay;

/**
 *
 * @author Chen Hongbao
 */
class TradingDayImpl implements TradingDay {

    private String day;
    private String updateTime;

    @Override
    public String getDay() {
        return day;
    }

    @Override
    public void setDay(String tradingDay) {
        this.day = tradingDay;
    }

    @Override
    public String getUpdateTime() {
        return updateTime;
    }

    @Override
    public void setUpdateTime(String updateTime) {
        this.updateTime = updateTime;
    }

}
