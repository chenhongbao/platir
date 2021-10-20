package io.platir.core.internal;

import io.platir.queries.Utils;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import io.platir.core.SettlementException;
import io.platir.service.Account;
import io.platir.service.Contract;
import io.platir.service.Instrument;
import io.platir.service.Order;
import io.platir.service.Position;
import io.platir.service.StrategyProfile;
import io.platir.service.Trade;
import io.platir.service.Transaction;
import io.platir.service.DataQueryException;
import io.platir.service.Queries;
import io.platir.service.PlatirInfoClient;
import java.util.logging.Logger;

class PlatirInfoClientImpl implements PlatirInfoClient {

    private final Queries queries;
    private final MarketRouter marketRouter;
    private final StrategyContextImpl strategyContext;
    private final Map<String, Instrument> instruments = new ConcurrentHashMap<>();
    private String whenQryTradingDay = null;
    private String tradingDay = null;

    PlatirInfoClientImpl(StrategyContextImpl strategyContext, MarketRouter marketRouter, Queries queries) {
        this.strategyContext = strategyContext;
        this.marketRouter = marketRouter;
        this.queries = queries;
    }

    @Override
    public Logger getLogger() {
        return strategyContext.getStrategyLogger();
    }

    protected StrategyContextImpl getStrategyContext() {
        return strategyContext;
    }

    Queries queries() {
        return queries;
    }

    @Override
    public String getStrategyId() {
        return getStrategyContext().getProfile().getStrategyId();
    }

    @Override
    public StrategyProfile getStrategyProfile() {
        return strategyContext.getProfile();
    }

    @Override
    public Account getAccount() {
        var userId = strategyContext.getProfile().getUserId();
        try {
            var snapshot = selectUserSnapshot(userId);
            SettlementFacilities.settleInDay(snapshot, getTradingDay(), marketRouter.getLastTicks(), queries.selectInstruments());
            return snapshot.getAccount();
        } catch (DataQueryException | SettlementException exception) {
            Utils.err().write("Fail querying account by user(" + userId + ").", exception);
        }
        return null;
    }

    private UserSnapshot selectUserSnapshot(String userId) throws DataQueryException, SettlementException {
        var snapshot = new UserSnapshot();
        for (var account : queries.selectAccounts()) {
            if (account.getUserId().equals(userId)) {
                snapshot.setAccount(account);
                break;
            }
        }
        for (var selectedUser : queries.selectUsers()) {
            if (selectedUser.getUserId().equals(userId)) {
                snapshot.setUser(selectedUser);
                break;
            }
        }
        for (var contract : queries.selectContracts()) {
            if (contract.getUserId().equals(userId)) {
                snapshot.contracts().computeIfAbsent(contract.getInstrumentId(), key -> new HashSet<Contract>()).add(contract);
            }
        }
        if (snapshot.getUser() == null) {
            throw new SettlementException("No user(" + userId + ") information.");
        }
        if (snapshot.getAccount() == null) {
            throw new SettlementException("No account for user(" + userId + ").");
        }
        return snapshot;
    }

    @Override
    public Instrument getInstrument(String instrumentId) {
        var instrument = instruments.get(instrumentId);
        if (instrument == null || !instrument.getUpdateTime().startsWith(Utils.date())) {
            return qryInstrument(instrumentId);
        } else {
            return instrument;
        }

    }

    private Instrument qryInstrument(String instrumentId) {
        try {
            for (var instrument : queries.selectInstruments()) {
                if (instrument.getInstrumentId().equals(instrumentId)) {
                    instrument.setUpdateTime(Utils.datetime());
                    instruments.put(instrumentId, instrument);
                    return instrument;
                }
            }
            return null;
        } catch (DataQueryException exception) {
            Utils.err().write("Fail querying instrument by ID(" + instrumentId + "): " + exception.getMessage(), exception);
            return null;
        }
    }

    @Override
    public Set<Transaction> getTransactions() {
        var transactions = new HashSet<Transaction>();
        var strategyId = getStrategyId();
        try {
            for (var transaction : queries.selectTransactions()) {
                if (transaction.getStrategyId().equals(strategyId)) {
                    transactions.add(transaction);
                }
            }
            return transactions;
        } catch (DataQueryException exception) {
            Utils.err().write("Fail querying transactions by strategy(" + getStrategyId() + "): " + exception.getMessage(), exception);
            return null;
        }
    }

    @Override
    public Set<Order> getOrders(String transactionId) {
        var orders = new HashSet<Order>();
        try {
            for (var order : queries.selectOrders()) {
                if (order.getTransactionId().equals(transactionId)) {
                    orders.add(order);
                }
            }
            return orders;
        } catch (DataQueryException exception) {
            Utils.err().write("Fail querying orders by transaction(" + transactionId + "): " + exception.getMessage(), exception);
            return null;
        }
    }

    @Override
    public Set<Trade> getTrades(String orderId) {
        var trades = new HashSet<Trade>();
        try {
            for (var trade : queries.selectTrades()) {
                if (trade.getOrderId().equals(orderId)) {
                    trades.add(trade);
                }
            }
            return trades;
        } catch (DataQueryException exception) {
            Utils.err().write("Fail querying trades by order(" + orderId + "): " + exception.getMessage(), exception);
            return null;
        }
    }

    @Override
    public Set<Position> getPositions(String... instrumentIds) {
        var instrumentPositions = new HashMap<String, InstrumentPosition>();
        var today = getTradingDay();
        var contracts = getContracts(instrumentIds);
        for (var contract : contracts) {
            var instrumentPosition = instrumentPositions.computeIfAbsent(contract.getInstrumentId(), key -> new InstrumentPosition(key, getStrategyProfile().getUserId()));
            Position position;
            if (contract.getDirection().compareToIgnoreCase("buy") == 0) {
                position = instrumentPosition.buy();
            } else if (contract.getDirection().compareToIgnoreCase("sell") == 0) {
                position = instrumentPosition.sell();
            } else {
                Utils.err().write("Invalid direction(" + contract.getDirection() + ") for contract(" + contract.getContractId() + ").");
                return null;
            }
            if (contract.getState().compareToIgnoreCase("opening") == 0) {
                position.setOpeningVolume(position.getOpeningVolume() + 1);
            } else if (contract.getState().compareToIgnoreCase("closing") == 0) {
                position.setClosingVolume(position.getClosingVolume() + 1);
            } else if (contract.getState().compareToIgnoreCase("open") == 0) {
                position.setOpenVolume(position.getOpenVolume() + 1);
                if (contract.getOpenTradingDay().equals(today)) {
                    position.setTodayOpenVolume(position.getTodayOpenVolume() + 1);
                }
            } else if (contract.getState().compareToIgnoreCase("closed") == 0) {
                position.setClosedVolume(position.getClosedVolume() + 1);
            } else {
                Utils.err().write("Invalid direction(" + contract.getDirection() + ") for contract(" + contract.getContractId() + ").");
            }
        }
        var positions = new HashSet<Position>();
        instrumentPositions.values().forEach(ip -> {
            positions.add(ip.buy());
            positions.add(ip.sell());
        });
        return positions;
    }

    @Override
    public Set<Contract> getContracts(String... instrumentIds) {
        /* find contracts that belong to the instrument and user */
        var contracts = new HashSet<Contract>();
        var instrumentLookup = new HashSet<String>(Arrays.asList(instrumentIds));
        var userId = getStrategyProfile().getUserId();
        String currentInstrumentId = null;
        try {
            for (var contract : queries.selectContracts()) {
                currentInstrumentId = contract.getInstrumentId();
                if (instrumentLookup.contains(currentInstrumentId) && contract.getUserId().equals(userId)) {
                    contracts.add(contract);
                }
            }
            return contracts;
        } catch (DataQueryException exception) {
            Utils.err().write("Fail querying contracts by instrument(" + currentInstrumentId + ").", exception);
            return null;
        }
    }

    @Override
    public String getTradingDay() {
        var today = Utils.date();
        if (whenQryTradingDay == null || !whenQryTradingDay.equals(today)) {
            return qryTradingDay();
        } else {
            return tradingDay;
        }
    }

    private String qryTradingDay() {
        try {
            tradingDay = queries.selectTradingDay().getTradingDay();
            whenQryTradingDay = Utils.date();
        } catch (DataQueryException e) {
            Utils.err().write("Fail querying trading day.", e);
            return null;
        }
        return tradingDay;
    }

    private class InstrumentPosition {

        private final Position buy;
        private final Position sell;

        InstrumentPosition(String instrumentId, String userId) {
            buy = createEmptyPosition(instrumentId, "buy", userId);
            sell = createEmptyPosition(instrumentId, "sell", userId);
        }

        private Position createEmptyPosition(String instrumentId, String direction, String userId) {
            var position = queries.getFactory().newPosition();
            position.setInstrumentId(instrumentId);
            position.setUserId(userId);
            position.setDirection(direction);
            position.setOpenVolume(0);
            position.setClosedVolume(0);
            position.setClosingVolume(0);
            position.setOpeningVolume(0);
            position.setTodayOpenVolume(0);
            return position;
        }

        public Position buy() {
            return buy;
        }

        public Position sell() {
            return sell;
        }

    }
}
