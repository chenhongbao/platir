package io.platir.core;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

class InfoHelper {

    private final AtomicReference<String> tradingDay = new AtomicReference<>();
    private final Map<String, SettlementPrice> settlementPrices = new ConcurrentHashMap<>();
    private final Map<String, InstrumentCore> instruments = new ConcurrentHashMap<>();

    String getTradingDay() {
        return tradingDay.get();
    }

    InstrumentCore getInstrument(String instrumentId) {
        return instruments.get(instrumentId);
    }

    Double getSettlementPriceOr(String instrumentId, Double orValue) {
        var settlementPrice = settlementPrices.get(instrumentId);
        if (settlementPrice == null || !settlementPrice.getTradingDay().equals(getTradingDay())) {
            return orValue;
        } else {
            return settlementPrice.getPrice();
        }
    }

    void setSettlementPrice(String instrumentId, Double price, String tradingDay) {
        var settlementPrice = settlementPrices.computeIfAbsent(instrumentId, key -> {
            return new SettlementPrice();
        });
        settlementPrice.setPrice(price);
        settlementPrice.setTradingDay(tradingDay);
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
}
