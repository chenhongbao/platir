package io.platir.queries;

import io.platir.service.TradingDay;

/**
 *
 * @author Chen Hongbao
 */
class TradingDayImpl implements TradingDay {

    private String tradingDay;
    private String updateTime;

    @Override
    public String getTradingDay() {
        return tradingDay;
    }

    @Override
    public void setTradingDay(String tradingDay) {
        this.tradingDay = tradingDay;
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
