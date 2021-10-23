package io.platir.core.internal;

import io.platir.queries.Utils;
import io.platir.service.DataQueryException;
import io.platir.service.Order;
import io.platir.service.Queries;
import io.platir.service.RiskNotice;
import io.platir.service.Tick;
import io.platir.service.Trade;
import io.platir.service.TransactionContext;
import io.platir.service.api.MarketAdapter;
import io.platir.service.api.MarketListener;
import io.platir.service.api.RiskManager;
import io.platir.service.api.ApiConstants;
import io.platir.service.api.TradeListener;
import java.util.Map;
import io.platir.service.api.TradeAdapter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

/**
 *
 * @author Chen Hongbao
 */
public class Simulators {

    public static class SimulatedTradeAdapter implements TradeAdapter {

        private final AtomicInteger tradeCounter = new AtomicInteger(0);
        private final Map<String, String> parameters = new HashMap<>();
        private final Map<String, Order> executingOrders = new HashMap<>();
        private final Queries queries;
        private TradeListener listener;
        private int requireReturnCode = ApiConstants.CODE_OK;

        public SimulatedTradeAdapter(Queries queries) {
            this.queries = queries;
        }

        @Override
        public int start() {
            return ApiConstants.CODE_OK;
        }

        @Override
        public Map<String, String> getParamaters() {
            return new HashMap<>(parameters);
        }

        @Override
        public void setParameters(Map<String, String> parameters) {
            this.parameters.putAll(parameters);
        }

        @Override
        public void setListener(TradeListener listener) {
            this.listener = listener;
        }

        @Override
        public int require(String orderId, String instrumentId, String offset, String direction, Double price, Integer volume) {
            var order = queries.getFactory().newOrder();
            order.setOrderId(orderId);
            order.setInstrumentId(instrumentId);
            order.setOffset(offset);
            order.setDirection(direction);
            order.setPrice(price);
            order.setVolume(volume);
            executingOrders.put(order.getOrderId(), order);
            return requireReturnCode;
        }

        public void setRequireReturnCode(int code) {
            this.requireReturnCode = code;
        }

        public Map<String, Order> executingOrders() {
            return executingOrders;
        }

        public void makeTrade(Order fillingOrder, int volume) {
            if (!executingOrders.containsKey(fillingOrder.getOrderId())) {
                throw new RuntimeException("Unknown order(" + fillingOrder.getOrderId() + ").");
            }
            callbackTrade(fillingOrder, volume);
            updateOrder(fillingOrder, volume);
        }

        private void updateOrder(Order fillingOrder, int volume) {
            fillingOrder.setVolume(fillingOrder.getVolume() - volume);
            if (fillingOrder.getVolume() <= 0) {
                executingOrders.remove(fillingOrder.getOrderId());
            }
        }

        private void callbackTrade(Order fillingOrder, int volume) {
            try {
                listener().onTrade(createTrade(fillingOrder, volume));
            } catch (Throwable throwable) {
                Utils.err().write("onTrade(Trade) throws exception: " + throwable.getMessage(), throwable);
            }
        }

        private Trade createTrade(Order fillingOrder, int volume) {
            var trade = queries.getFactory().newTrade();
            trade.setTradeId(Utils.date() + "." + tradeCounter.addAndGet(1));
            trade.setDirection(fillingOrder.getDirection());
            trade.setInstrumentId(fillingOrder.getInstrumentId());
            trade.setOffset(fillingOrder.getOffset());
            trade.setOrderId(fillingOrder.getOrderId());
            trade.setPrice(fillingOrder.getPrice());
            trade.setUpdateTime(Utils.datetime());
            trade.setVolume(volume);
            try {
                trade.setTradingDay(queries.selectTradingDay().getDay());
            } catch (DataQueryException exception) {
                Utils.err().write("Fail querying trading day: " + exception.getMessage(), exception);
            }
            return trade;
        }

        private TradeListener listener() {
            if (listener == null) {
                throw new NullPointerException("Trade listener null pointer.");
            }
            return listener;
        }
    }

    public static class SimulatedMarketAdapter implements MarketAdapter {

        private final Map<String, String> parameters = new HashMap<>();
        private final Set<String> subscription = new HashSet<>();
        private MarketListener listener;

        @Override
        public int start() {
            return ApiConstants.CODE_OK;
        }

        @Override
        public Map<String, String> getParamaters() {
            return new HashMap<>(parameters);
        }

        @Override
        public void setParameters(Map<String, String> parameters) {
            this.parameters.putAll(parameters);
        }

        @Override
        public void setListener(MarketListener listener) {
            this.listener = listener;
        }

        @Override
        public int subscribe(String instrumentId) {
            subscription.add(instrumentId);
            return ApiConstants.CODE_OK;
        }

        public Set<String> subscription() {
            return new HashSet<>(subscription);
        }

        public void publish(Tick tick) {
            try {
                listener().onTick(tick);
            } catch (Throwable throwable) {
                Utils.err().write("onTick(Tick) throws exception: " + throwable.getMessage(), throwable);
            }
        }

        private MarketListener listener() {
            if (listener == null) {
                throw new NullPointerException("Market listener null pointer.");
            }
            return listener;
        }
    }

    public static class SimulatedRiskManager implements RiskManager {

        private final Map<Tick, TransactionContext> befores = new HashMap<>();
        private final Map<Trade, TransactionContext> afters = new HashMap<>();
        private final Queries queries;

        private int code;

        public SimulatedRiskManager(Queries queries) {
            this.queries = queries;
        }

        @Override
        public RiskNotice before(Tick current, TransactionContext transaction) {
            befores.put(current, transaction);
            return createNotice();
        }

        @Override
        public RiskNotice after(Trade trade, TransactionContext transaction) {
            afters.put(trade, transaction);
            return createNotice();
        }

        public void setCode(int code) {
            this.code = code;
        }

        private RiskNotice createNotice() {
            var riskNotice = queries.getFactory().newRiskNotice();
            riskNotice.setCode(code);
            riskNotice.setMessage("simulater");
            return riskNotice;
        }

    }
}
