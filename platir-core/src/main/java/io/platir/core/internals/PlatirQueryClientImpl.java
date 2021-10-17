package io.platir.core.internals;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import io.platir.core.PlatirSystem;
import io.platir.core.SettlementException;
import io.platir.core.internals.SettlementFacilities.UserSnapshot;
import io.platir.core.internals.persistence.object.ObjectFactory;
import io.platir.service.Account;
import io.platir.service.Contract;
import io.platir.service.Instrument;
import io.platir.service.Order;
import io.platir.service.PlatirQueryClient;
import io.platir.service.Position;
import io.platir.service.StrategyProfile;
import io.platir.service.Trade;
import io.platir.service.Transaction;
import io.platir.service.api.DataQueryException;
import io.platir.service.api.Queries;

class PlatirQueryClientImpl implements PlatirQueryClient {

    private final Queries qry;
    private final MarketRouter market;
    private final StrategyContextImpl stg;
    private final Map<String, Instrument> instruments = new ConcurrentHashMap<>();
    private String sid;
    private String whenQryTradingDay = null;
    private String tradingDay = null;

    PlatirQueryClientImpl(StrategyContextImpl strategyContext, MarketRouter mkRouter, Queries queries) {
        stg = strategyContext;
        market = mkRouter;
        qry = queries;
    }

    protected StrategyContextImpl getStrategyContext() {
        return stg;

    }

    Queries queries() {
        return qry;
    }

    @Override
    public String getStrategyId() {
        if (sid == null) {
            sid = getStrategyContext().getProfile().getStrategyId();
        }
        return sid;
    }

    @Override
    public StrategyProfile getStrategyProfile() {
        return stg.getProfile();
    }

    @Override
    public Account getAccount() {
        var uid = stg.getProfile().getUserId();
        try {
            var snapshot = selectUserSnapshot(uid);
            new SettlementFacilities().settleInDay(snapshot, getTradingDay(), market.getLastTicks(),
                    qry.selectInstruments());
            return snapshot.account;
        } catch (DataQueryException | SettlementException e) {
            PlatirSystem.err.write("Fail querying account by user(" + uid + ").", e);
        }
        return null;
    }

    private UserSnapshot selectUserSnapshot(String userId) throws DataQueryException, SettlementException {
        var user = new UserSnapshot();
        for (var a : qry.selectAccounts()) {
            if (a.getUserId().equals(userId)) {
                user.account = a;
                break;
            }
        }
        for (var u : qry.selectUsers()) {
            if (u.getUserId().equals(userId)) {
                user.user = u;
                break;
            }
        }
        for (var c : qry.selectContracts()) {
            if (c.getUserId().equals(userId)) {
                user.contracts.computeIfAbsent(c.getInstrumentId(), key -> new HashSet<Contract>()).add(c);
            }
        }
        if (user.user == null) {
            throw new SettlementException("No user(" + userId + ") information.");
        }
        if (user.account == null) {
            throw new SettlementException("No account for user(" + userId + ").");
        }
        return user;
    }

    @Override
    public Instrument getInstrument(String instrumentId) {
        var inst = instruments.get(instrumentId);
        if (inst == null || !inst.getUpdateTime().startsWith(PlatirSystem.date())) {
            return qryInstrument(instrumentId);
        } else {
            return inst;
        }

    }

    private Instrument qryInstrument(String instrumentId) {
        try {
            for (var i : qry.selectInstruments()) {
                if (i.getInstrumentId().equals(instrumentId)) {
                    i.setUpdateTime(PlatirSystem.datetime());
                    instruments.put(instrumentId, i);
                    return i;
                }
            }
            return null;
        } catch (DataQueryException e) {
            PlatirSystem.err.write("Fail querying instrument by ID(" + instrumentId + ").", e);
            return null;
        }
    }

    @Override
    public Set<Transaction> getTransactions() {
        var r = new HashSet<Transaction>();
        var strategyId = getStrategyId();
        try {
            for (var t : qry.selectTransactions()) {
                if (t.getStrategyId().equals(strategyId)) {
                    r.add(t);
                }
            }
            return r;
        } catch (DataQueryException e) {
            PlatirSystem.err.write("Fail querying transactions by strategy(" + getStrategyId() + ").", e);
            return null;
        }
    }

    @Override
    public Set<Order> getOrders(String transactionId) {
        var r = new HashSet<Order>();
        try {
            for (var order : qry.selectOrders()) {
                if (order.getTransactionId().equals(transactionId)) {
                    r.add(order);
                }
            }
            return r;
        } catch (DataQueryException e) {
            PlatirSystem.err.write("Fail querying orders by transaction(" + transactionId + ").", e);
            return null;
        }
    }

    @Override
    public Set<Trade> getTrades(String orderId) {
        var r = new HashSet<Trade>();
        try {
            for (var tr : qry.selectTrades()) {
                if (tr.getOrderId().equals(orderId)) {
                    r.add(tr);
                }
            }
            return r;
        } catch (DataQueryException e) {
            PlatirSystem.err.write("Fail querying trades by order(" + orderId + ").", e);
            return null;
        }
    }

    @Override
    public Set<Position> getPositions(String... instrumentIds) {
        var r = new HashMap<String, InstrumentPosition>();
        var trDay = getTradingDay();
        var contracts = getContracts(instrumentIds);
        for (var c : contracts) {
            var ip = r.computeIfAbsent(c.getInstrumentId(),
                    key -> new InstrumentPosition(key, getStrategyProfile().getUserId()));
            Position p;
            if (c.getDirection().compareToIgnoreCase("buy") == 0) {
                p = ip.buy();
            } else if (c.getDirection().compareToIgnoreCase("sell") == 0) {
                p = ip.sell();
            } else {
                PlatirSystem.err
                        .write("Invalid direction(" + c.getDirection() + ") for contract(" + c.getContractId() + ").");
                return null;
            }
            if (c.getState().compareToIgnoreCase("opening") == 0) {
                p.setOpeningVolume(p.getOpeningVolume() + 1);
            } else if (c.getState().compareToIgnoreCase("closing") == 0) {
                p.setClosingVolume(p.getClosingVolume() + 1);
            } else if (c.getState().compareToIgnoreCase("open") == 0) {
                p.setOpenVolume(p.getOpenVolume() + 1);
                if (c.getOpenTradingDay().equals(trDay)) {
                    p.setTodayOpenVolume(p.getTodayOpenVolume() + 1);
                }
            } else if (c.getState().compareToIgnoreCase("closed") == 0) {
                p.setClosedVolume(p.getClosedVolume() + 1);
            } else {
                PlatirSystem.err
                        .write("Invalid direction(" + c.getDirection() + ") for contract(" + c.getContractId() + ").");
            }
        }
        var rs = new HashSet<Position>();
        r.values().forEach(ip -> {
            rs.add(ip.buy());
            rs.add(ip.sell());
        });
        return rs;
    }

    @Override
    public Set<Contract> getContracts(String... instrumentIds) {
        /* find contracts that belong to the instrument and user */
        var r = new HashSet<Contract>();
        var i = new HashSet<String>(Arrays.asList(instrumentIds));
        var userId = getStrategyProfile().getUserId();
        try {
            for (var c : qry.selectContracts()) {
                if (i.contains(c.getInstrumentId()) && c.getUserId().equals(userId)) {
                    r.add(c);
                }
            }
            return r;
        } catch (DataQueryException e) {
            PlatirSystem.err.write("Fail querying contracts by instrument(" + i + ").", e);
            return null;
        }
    }

    @Override
    public String getTradingDay() {
        var today = PlatirSystem.date();
        if (whenQryTradingDay == null || !whenQryTradingDay.equals(today)) {
            return qryTradingDay();
        } else {
            return tradingDay;
        }
    }

    private String qryTradingDay() {
        try {
            tradingDay = qry.selectTradingDay().getTradingDay();
            whenQryTradingDay = PlatirSystem.date();
        } catch (DataQueryException e) {
            PlatirSystem.err.write("Fail querying trading day.", e);
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
            var p = ObjectFactory.newPosition();
            p.setInstrumentId(instrumentId);
            p.setUserId(userId);
            p.setDirection(direction);
            p.setOpenVolume(0);
            p.setClosedVolume(0);
            p.setClosingVolume(0);
            p.setOpeningVolume(0);
            p.setTodayOpenVolume(0);
            return p;
        }

        public Position buy() {
            return buy;
        }

        public Position sell() {
            return sell;
        }

    }
}
