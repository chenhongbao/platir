package io.platir.core.internals;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import io.platir.core.BrokenSettingException;
import io.platir.core.PlatirSystem;
import io.platir.core.SettlementException;
import io.platir.service.Account;
import io.platir.service.Contract;
import io.platir.service.Instrument;
import io.platir.service.Tick;
import io.platir.service.User;

class SettlementFacilities {

	static class UserSnapshot {
		User user;
		Account account;
		Map<String, Set<Contract>> contracts = new HashMap<>();
	}

	static Double computeRatio(Double price, Double multi, Double byAmount, Double byVolume)
			throws BrokenSettingException {
		if (byAmount == 0 && byVolume > 0) {
			return byVolume;
		} else if (byAmount > 0 && byVolume == 0) {
			return price * byAmount * multi;
		} else {
			throw new BrokenSettingException("Illegal margin/commission setting.");
		}
	}

	void settleInDay(UserSnapshot user, String tradingDay, Collection<Tick> ticks, Collection<Instrument> instruments)
			throws SettlementException {
		var tm = tickMap(ticks);
		var im = instrumentMap(instruments);
		resetAccount(user.account, tradingDay);
		for (var entry : user.contracts.entrySet()) {
			settleInstrumentInDay(user.account, entry.getValue(), getTick(entry.getKey(), tm),
					getInstrument(entry.getKey(), im));
		}
		finishSettlement(user.account);
	}

	void settle(UserSnapshot user, String tradingDay, Collection<Tick> ticks, Collection<Instrument> instruments)
			throws SettlementException {
		var tm = tickMap(ticks);
		var im = instrumentMap(instruments);
		var account = user.account;
		resetAccount(account, tradingDay);
		for (var entry : user.contracts.entrySet()) {
			settleInstrument(user.account, entry.getValue(), getTick(entry.getKey(), tm),
					getInstrument(entry.getKey(), im));
		}
		finishSettlement(user.account);
	}

	private void finishSettlement(Account account) {
		account.setBalance(account.getYdBalance() + account.getCloseProfit() + account.getPositionProfit()
				- account.getCommission());
		account.setAvailable(account.getBalance() - account.getMargin() - account.getOpeningMargin()
				- account.getOpeningCommission() - account.getCommission() - account.getClosingCommission());
	}

	private void settleInstrument(Account account, Set<Contract> contracts, Tick tick, Instrument instrument)
			throws SettlementException {
		var itr = contracts.iterator();
		while (itr.hasNext()) {
			var c = itr.next();
			var state = c.getState();
			if (state.compareToIgnoreCase("closing") == 0) {
				c.setState("open");
			} else if (state.compareToIgnoreCase("opening") == 0) {
				itr.remove();
				continue;
			}
			var settlementPrice = tick.getLastPrice();
			settleContract(account, c, settlementPrice, instrument);
			/* set (open)price to settlement price of today */
			c.setPrice(settlementPrice);
			if (state.compareToIgnoreCase("closed") == 0) {
				itr.remove();
			}
		}
	}

	private void settleInstrumentInDay(Account account, Set<Contract> contracts, Tick tick, Instrument instrument)
			throws SettlementException {
		var itr = contracts.iterator();
		while (itr.hasNext()) {
			var c = itr.next();
			settleContract(account, c, tick.getLastPrice(), instrument);
			settleContract0(account, c, tick.getLastPrice(), instrument);
		}
	}

	private void settleContract(Account account, Contract c, Double settlementPrice, Instrument instrument)
			throws SettlementException {
		var state = c.getState();
		if (state.compareToIgnoreCase("open") == 0) {
			account.setMargin(account.getMargin() + margin(c, instrument));
			account.setPositionProfit(account.getPositionProfit() + positionProfit(settlementPrice, c, instrument));
			if (c.getOpenTradingDay().equals(account.getTradingDay())) {
				/* today open commission */
				account.setCommission(account.getCommission() + commission(c, instrument));
			}
		} else if (state.compareToIgnoreCase("closed") == 0) {
			account.setCloseProfit(account.getCloseProfit() + closeProfit(c, instrument));
			account.setCommission(account.getCommission() + commission(c, instrument));
		}
	}

	private void settleContract0(Account account, Contract c, Double settlementPrice, Instrument instrument)
			throws SettlementException {
		var state = c.getState();
		if (state.compareToIgnoreCase("opening") == 0) {
			account.setOpeningMargin(account.getOpeningMargin() + margin(c, instrument));
			account.setOpeningCommission(account.getOpeningCommission() + commission(c, instrument));
		} else if (state.compareToIgnoreCase("closing") == 0) {
			account.setMargin(account.getMargin() + margin(c, instrument));
			account.setClosingCommission(account.getCommission() + commission(c, instrument));
			account.setPositionProfit(account.getPositionProfit() + positionProfit(settlementPrice, c, instrument));
		}
	}

	private Double margin(Contract c, Instrument instrument) {
		return computeRatio(c.getPrice(), instrument.getMultiple(), instrument.getAmountMargin(),
				instrument.getVolumeMargin());
	}

	private Double positionProfit(Double settlementPrice, Contract c, Instrument instrument)
			throws SettlementException {
		if (c.getClosePrice() != null && c.getClosePrice() != 0) {
			throw new SettlementException("Open(closing) contract(" + c.getContractId() + ") has close price.");
		}
		return profit(c, settlementPrice, instrument.getMultiple());
	}

	private Double commission(Contract c, Instrument instrument) {
		return computeRatio(c.getPrice(), instrument.getMultiple(), instrument.getAmountCommission(),
				instrument.getVolumeCommission());
	}

	private Double closeProfit(Contract c, Instrument instrument) throws SettlementException {
		if (c.getClosePrice() == null || c.getClosePrice() == 0) {
			throw new SettlementException("Closed contract(" + c.getContractId() + ") has no close price.");
		}
		return profit(c, c.getClosePrice(), instrument.getMultiple());
	}

	private Double profit(Contract c, Double closePrice, Double multiple) throws SettlementException {
		var direction = c.getDirection();
		var price = c.getPrice();
		if (direction.compareToIgnoreCase("buy") == 0) {
			return (closePrice - price) * multiple;
		} else if (direction.compareToIgnoreCase("sell") == 0) {
			return (price - closePrice) * multiple;
		} else {
			throw new SettlementException(
					"Contract(" + c.getContractId() + ") has invalid direction(" + direction + ").");
		}
	}

	private void resetAccount(Account account, String tradingDay) {
		account.setYdBalance(account.getBalance());
		account.setBalance(0D);
		account.setMargin(0D);
		account.setCommission(0D);
		account.setOpeningMargin(0D);
		account.setOpeningCommission(0D);
		account.setClosingCommission(0D);
		account.setAvailable(0D);
		account.setPositionProfit(0D);
		account.setCloseProfit(0D);
		account.setTradingDay(tradingDay);
		account.setSettleTime(PlatirSystem.datetime());
	}

	HashMap<String, UserSnapshot> users(Set<User> users, Set<Account> accounts, Set<Contract> contracts)
			throws SettlementException {
		var m = new HashMap<String, UserSnapshot>();
		var userItr = users.iterator();
		while (userItr.hasNext()) {
			var u = userItr.next();
			if (m.containsKey(u.getUserId())) {
				throw new SettlementException("Duplicated user(" + u.getUserId() + ").");
			}
			var o = m.computeIfAbsent(u.getUserId(), key -> new UserSnapshot());
			o.user = u;
			accounts(o, accounts, contracts);
			userItr.remove();
		}
		return m;
	}

	void accounts(UserSnapshot o, Set<Account> accounts, Set<Contract> contracts) throws SettlementException {
		var u = o.user;
		/* find user's account */
		var accItr = accounts.iterator();
		while (accItr.hasNext()) {
			var a = accItr.next();
			if (a.getUserId().equals(u.getUserId())) {
				if (o.account != null) {
					throw new SettlementException("Duplicated account(" + a.getAccountId() + "/"
							+ o.account.getAccountId() + ") for user(" + u.getUserId() + ").");
				}
				o.account = a;
			}
			accItr.remove();
		}
		contracts(o, contracts);
	}

	void contracts(UserSnapshot o, Set<Contract> contracts) {
		/* find user's contracts */
		var cItr = contracts.iterator();
		while (cItr.hasNext()) {
			var c = cItr.next();
			if (c.getUserId().equals(o.user.getUserId())) {
				o.contracts.computeIfAbsent(c.getInstrumentId(), key -> new HashSet<Contract>()).add(c);
			}
			cItr.remove();
		}
	}

	private Map<String, Tick> tickMap(Collection<Tick> ticks) {
		var m = new HashMap<String, Tick>();
		ticks.forEach(t -> {
			m.put(t.getInstrumentId(), t);
		});
		return m;
	}

	private Map<String, Instrument> instrumentMap(Collection<Instrument> instruments) {
		var m = new HashMap<String, Instrument>();
		instruments.forEach(i -> {
			m.put(i.getInstrumentId(), i);
		});
		return m;
	}

	private Tick getTick(String instrumentId, Map<String, Tick> ticks) throws SettlementException {
		var t = ticks.get(instrumentId);
		if (t == null) {
			throw new SettlementException("Tick(" + instrumentId + ") not found.");
		}
		return t;
	}

	private Instrument getInstrument(String instrumentId, Map<String, Instrument> instruments)
			throws SettlementException {
		var i = instruments.get(instrumentId);
		if (i == null) {
			throw new SettlementException("Instrument(" + instrumentId + ") not found.");
		}
		return i;
	}
}