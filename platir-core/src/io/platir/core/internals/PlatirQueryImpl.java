package io.platir.core.internals;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import io.platir.core.PlatirSystem;
import io.platir.service.Account;
import io.platir.service.Contract;
import io.platir.service.Instrument;
import io.platir.service.Order;
import io.platir.service.PlatirQuery;
import io.platir.service.Position;
import io.platir.service.StrategyProfile;
import io.platir.service.Trade;
import io.platir.service.Transaction;
import io.platir.service.api.Queries;

class PlatirQueryImpl implements PlatirQuery {

	private final Queries queries;
	private final StrategyContextImpl stg;
	private final Map<String, Instrument> instruments = new ConcurrentHashMap<>();
	private String sid;
	private String whenQryTradingDay = null;
	private String tradingDay = null;

	PlatirQueryImpl(StrategyContextImpl strategyContext, Queries queries) {
		stg = strategyContext;
		this.queries = queries;
	}

	protected StrategyContextImpl getStrategyContext() {
		return stg;

	}

	void prepareTables() {
		// TODO
	}

	@Override
	public String getStrategyId() {
		if (sid == null) {
			sid = getStrategyContext().getPofile().getStrategyId();
		}
		return sid;
	}

	@Override
	public StrategyProfile getStrategyProfile() {
		return stg.getPofile();
	}

	@Override
	public Account getAccount() {
		var uid = stg.getPofile().getUserId();
		try {
			for (var a : queries.selectAccounts()) {
				if (a.getUserId().equals(uid)) {
					return a;
				}
			}
		} catch (SQLException e) {
			PlatirSystem.err.write("Fail querying account by user(" + uid + ").", e);
		}
		return null;
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
			for (var i : queries.selectInstruments()) {
				if (i.getInstrumentId().equals(instrumentId)) {
					i.setUpdateTime(PlatirSystem.datetime());
					instruments.put(instrumentId, i);
					return i;
				}
			}
			return null;
		} catch (SQLException e) {
			PlatirSystem.err.write("Fail querying instrument by ID(" + instrumentId + ").", e);
			return null;
		}
	}

	@Override
	public Set<Transaction> getTransactions() {
		var r = new HashSet<Transaction>();
		var strategyId = getStrategyId();
		try {
			for (var t : queries.selectTransactions()) {
				if (t.getStrategyId().equals(strategyId)) {
					r.add(t);
				}
			}
			return r;
		} catch (SQLException e) {
			PlatirSystem.err.write("Fail querying transactions by strategy(" + getStrategyId() + ").", e);
			return null;
		}
	}

	@Override
	public Set<Order> getOrders(String transactionId) {
		var r = new HashSet<Order>();
		try {
			for (var order : queries.selectOrders()) {
				if (order.getTransactionId().equals(transactionId)) {
					r.add(order);
				}
			}
			return r;
		} catch (SQLException e) {
			PlatirSystem.err.write("Fail querying orders by transaction(" + transactionId + ").", e);
			return null;
		}
	}

	@Override
	public Set<Trade> getTrades(String orderId) {
		var r = new HashSet<Trade>();
		try {
			for (var tr : queries.selectTrades()) {
				if (tr.getOrderId().equals(orderId)) {
					r.add(tr);
				}
			}
			return r;
		} catch (SQLException e) {
			PlatirSystem.err.write("Fail querying trades by order(" + orderId + ").", e);
			return null;
		}
	}

	@Override
	public Set<Position> getPositions(String... instrumentIds) {
		var r = new HashMap<String, InstrumentPosition>();
		var tradingDay = getTradingDay();
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
				if (c.getOpenTradingDay().equals(tradingDay)) {
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
		var r = new HashSet<Contract>();
		var i = new HashSet<String>(Arrays.asList(instrumentIds));
		try {
			for (var c : queries.selectContracts()) {
				if (i.contains(c.getInstrumentId())) {
					r.add(c);
				}
			}
			return r;
		} catch (SQLException e) {
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
			tradingDay = queries.selectTradingday();
			whenQryTradingDay = PlatirSystem.date();
		} catch (SQLException e) {
			PlatirSystem.err.write("Fail querying trading day.", e);
			return null;
		}
		return tradingDay;
	}

	void update(Transaction transaction) {
		try {
			queries.update(transaction);
		} catch (SQLException e) {
			PlatirSystem.err.write("Fail updating transaction(" + transaction.getTransactionId() + ").", e);
		}
	}

	void add(Contract contract) {
		try {
			queries.insert(contract);
		} catch (SQLException e) {
			PlatirSystem.err.write("Fail inserting contract(" + contract.getContractId() + ").", e);
		}
	}

	void update(Contract contract) {
		try {
			queries.update(contract);
		} catch (SQLException e) {
			PlatirSystem.err.write("Fail updating contract(" + contract.getContractId() + ").", e);
		}
	}

	void map(Order order, Contract contract) {
		try {
			queries.oneToMany(order, contract);
		} catch (SQLException e) {
			PlatirSystem.err.write(
					"Fail inserting new mapping(" + order.getOrderId() + "->" + contract.getContractId() + ").", e);
		}
	}

	void add(Trade trade) {
		try {
			queries.insert(trade);
		} catch (SQLException e) {
			PlatirSystem.err.write("Fail inserting new trade(" + trade.getTradeId() + ").", e);
		}
	}

	void add(Order order) {
		try {
			queries.insert(order);
		} catch (SQLException e) {
			PlatirSystem.err.write("Fail inserting new order(" + order.getOrderId() + ").", e);
		}
	}

	void add(Transaction transaction) {
		try {
			queries.insert(transaction);
		} catch (SQLException e) {
			PlatirSystem.err.write("Fail inserting new transaction(" + transaction.getTransactionId() + ").", e);
		}
	}

	private class InstrumentPosition {
		private Position buy;
		private Position sell;

		InstrumentPosition(String instrumentId, String userId) {
			buy = createEmptyPosition(instrumentId, "buy", userId);
			sell = createEmptyPosition(instrumentId, "sell", userId);
		}

		private Position createEmptyPosition(String instrumentId, String direction, String userId) {
			var p = new Position();
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
