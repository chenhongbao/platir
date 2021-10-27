package io.platir.engine.core;

import io.platir.util.Utils;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

class InfoCenter {

    private static final AtomicReference<String> tradingDay = new AtomicReference<>();
    private static final Map<String, SettlementPrice> settlementPrices = new ConcurrentHashMap<>();
    private static final Map<String, LatestPrice> latestPrices = new ConcurrentHashMap<>();
    private static final Map<String, InstrumentCore> instruments = new ConcurrentHashMap<>();

    static String getTradingDay() throws InsufficientInfoException {
        synchronized (tradingDay) {
            if (tradingDay.get() == null || tradingDay.get().compareTo(Utils.date()) < 0) {
                throw new InsufficientInfoException("No trading day.");
            }
            return tradingDay.get();
        }
    }

    static InstrumentCore getInstrument(String instrumentId) throws InsufficientInfoException {
        synchronized (instruments) {
            if (!instruments.containsKey(instrumentId)) {
                throw new InsufficientInfoException("No instrument for " + instrumentId + ".");
            }
            return instruments.get(instrumentId);
        }
    }

    static Double getSettlementPriceOr(String instrumentId, Double orValue) {
        synchronized (settlementPrices) {
            var settlementPrice = settlementPrices.get(instrumentId);
            if (settlementPrice == null || settlementPrice.getTradingDay().compareTo(Utils.date()) < 0) {
                return orValue;
            } else {
                return settlementPrice.getPrice();
            }
        }
    }

    static Double getLatestPrice(String instrumentId) throws InsufficientInfoException {
        synchronized (latestPrices) {
            if (!latestPrices.containsKey(instrumentId)) {
                throw new InsufficientInfoException("No latest price for " + instrumentId + ".");
            }
            return latestPrices.get(instrumentId).getPrice();
        }
    }

    static void setSettlementPrice(String instrumentId, Double price, String tradingDay) {
        synchronized (settlementPrices) {
            var settlementPrice = settlementPrices.computeIfAbsent(instrumentId, key -> new SettlementPrice());
            settlementPrice.setPrice(price);
            settlementPrice.setTradingDay(tradingDay);
        }
    }

    static void setLatestPrice(String instrumentId, Double price) {
        synchronized (latestPrices) {
            var latestPrice = latestPrices.computeIfAbsent(instrumentId, key -> new LatestPrice());
            latestPrice.setPrice(price);
            latestPrice.setUpdateDateTime(Utils.datetime());
        }
    }

    static void setInstrument(InstrumentCore instrument) {
        synchronized (instruments) {
            if (instrument != null) {
                instruments.put(instrument.getInstrumentId(), instrument);
            }
        }
    }

    /**
     * Set trading day.
     * <p>
     * The method is called internally by re-initialization daemon at the
     * begning of every trading day.
     *
     * @param day trading day string
     */
    static void setTradingDay(String day) {
        synchronized (tradingDay) {
            tradingDay.set(day);
        }
    }

    private static class SettlementPrice {

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

    private static class LatestPrice {

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
