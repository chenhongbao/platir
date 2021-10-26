package io.platir.engine.core;

import io.platir.util.Utils;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

class InfoHelper {

    private final AtomicReference<String> tradingDay = new AtomicReference<>();
    private final Map<String, SettlementPrice> settlementPrices = new ConcurrentHashMap<>();
    private final Map<String, LatestPrice> latestPrices = new ConcurrentHashMap<>();
    private final Map<String, InstrumentCore> instruments = new ConcurrentHashMap<>();

    String getTradingDay() throws InsufficientInfoException {
        if (tradingDay.get() == null || tradingDay.get().compareTo(Utils.date()) < 0) {
            throw new InsufficientInfoException("No trading day.");
        }
        return tradingDay.get();
    }

    InstrumentCore getInstrument(String instrumentId) throws InsufficientInfoException {
        if (!instruments.containsKey(instrumentId)) {
            throw new InsufficientInfoException("No instrument for " + instrumentId + ".");
        }
        return instruments.get(instrumentId);
    }

    Double getSettlementPriceOr(String instrumentId, Double orValue) {
        var settlementPrice = settlementPrices.get(instrumentId);
        if (settlementPrice == null || settlementPrice.getTradingDay().compareTo(Utils.date()) < 0) {
            return orValue;
        } else {
            return settlementPrice.getPrice();
        }
    }

    Double getLatestPrice(String instrumentId) throws InsufficientInfoException {
        if (!latestPrices.containsKey(instrumentId)) {
            throw new InsufficientInfoException("No latest price for " + instrumentId + ".");
        }
        return latestPrices.get(instrumentId).getPrice();
    }

    void setSettlementPrice(String instrumentId, Double price, String tradingDay) {
        var settlementPrice = settlementPrices.computeIfAbsent(instrumentId, key -> new SettlementPrice());
        settlementPrice.setPrice(price);
        settlementPrice.setTradingDay(tradingDay);
    }

    void setLatestPrice(String instrumentId, Double price) {
        var latestPrice = latestPrices.computeIfAbsent(instrumentId, key -> new LatestPrice());
        latestPrice.setPrice(price);
        latestPrice.setUpdateDateTime(Utils.datetime());
    }

    void setInstrument(InstrumentCore instrument) {
        if (instrument != null) {
            instruments.put(instrument.getInstrumentId(), instrument);
        }
    }

    void setTradingDay(String tradingDay) {
        this.tradingDay.set(tradingDay);
    }

    private class SettlementPrice {

        private Double price;
        private String tradingDay;

        public Double getPrice() {
            return price;
        }

        public void setPrice(Double price) {
            this.price = price;
        }

        public String getTradingDay() {
            return tradingDay;
        }

        public void setTradingDay(String tradingDay) {
            this.tradingDay = tradingDay;
        }

    }

    private class LatestPrice {

        private Double price;
        private String updateDateTime;

        public Double getPrice() {
            return price;
        }

        public void setPrice(Double price) {
            this.price = price;
        }

        public String getUpdateTime() {
            return updateDateTime;
        }

        public void setUpdateDateTime(String updateTime) {
            this.updateDateTime = updateTime;
        }

    }
}
