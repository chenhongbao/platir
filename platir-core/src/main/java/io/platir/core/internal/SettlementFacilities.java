package io.platir.core.internal;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import io.platir.core.BrokenSettingException;
import io.platir.core.SettlementException;
import io.platir.service.Account;
import io.platir.service.Contract;
import io.platir.service.Instrument;
import io.platir.service.Tick;
import io.platir.service.User;

class SettlementFacilities {

    static Double computeRatio(Double price, Double multi, Double byAmount, Double byVolume) throws BrokenSettingException {
        if (byAmount == 0 && byVolume > 0) {
            return byVolume;
        } else if (byAmount > 0 && byVolume == 0) {
            return price * byAmount * multi;
        } else {
            throw new BrokenSettingException("Illegal margin/commission setting.");
        }
    }

    static void settleInDay(UserSnapshot user, String tradingDay, Collection<Tick> ticks, Collection<Instrument> instruments) throws SettlementException {
        var tickLookup = tickMap(ticks);
        var instrumentLookup = instrumentMap(instruments);
        resetAccount(user.getAccount(), tradingDay);
        for (var entry : user.contracts().entrySet()) {
            settleInstrumentInDay(user.getAccount(), entry.getValue(), getTick(entry.getKey(), tickLookup),
                    getInstrument(entry.getKey(), instrumentLookup));
        }
        finishSettlement(user.getAccount());
    }

    static void settle(UserSnapshot user, String tradingDay, Collection<Tick> ticks, Collection<Instrument> instruments) throws SettlementException {
        var tickLookup = tickMap(ticks);
        var instrumentLookup = instrumentMap(instruments);
        resetAccount(user.getAccount(), tradingDay);
        for (var entry : user.contracts().entrySet()) {
            settleInstrument(user.getAccount(), entry.getValue(), getTick(entry.getKey(), tickLookup),
                    getInstrument(entry.getKey(), instrumentLookup));
        }
        finishSettlement(user.getAccount());
    }

    private static void finishSettlement(Account account) {
        account.setBalance(account.getYdBalance() + account.getCloseProfit() + account.getPositionProfit() - account.getCommission());
        account.setAvailable(account.getBalance() - account.getMargin() - account.getOpeningMargin() - account.getOpeningCommission() - account.getCommission() - account.getClosingCommission());
    }

    private static void settleInstrument(Account account, Set<Contract> contracts, Tick tick, Instrument instrument) throws SettlementException {
        var contractInterator = contracts.iterator();
        while (contractInterator.hasNext()) {
            var contract = contractInterator.next();
            var state = contract.getState();
            if (state.compareToIgnoreCase("closing") == 0) {
                contract.setState("open");
            } else if (state.compareToIgnoreCase("opening") == 0) {
                contractInterator.remove();
                continue;
            }
            var settlementPrice = tick.getLastPrice();
            settleContract(account, contract, settlementPrice, instrument);
            /* set (open)price to settlement price of today */
            contract.setPrice(settlementPrice);
            if (state.compareToIgnoreCase("closed") == 0) {
                contractInterator.remove();
            }
        }
    }

    private static void settleInstrumentInDay(Account account, Set<Contract> contracts, Tick tick, Instrument instrument) throws SettlementException {
        var contractIterator = contracts.iterator();
        while (contractIterator.hasNext()) {
            var contract = contractIterator.next();
            settleContract(account, contract, tick.getLastPrice(), instrument);
            settleContract0(account, contract, tick.getLastPrice(), instrument);
        }
    }

    private static void settleContract(Account account, Contract contract, Double settlementPrice, Instrument instrument) throws SettlementException {
        var state = contract.getState();
        if (state.compareToIgnoreCase("open") == 0) {
            account.setMargin(account.getMargin() + margin(contract, instrument));
            account.setPositionProfit(account.getPositionProfit() + positionProfit(settlementPrice, contract, instrument));
            if (contract.getOpenTradingDay().equals(account.getTradingDay())) {
                /* today open commission */
                account.setCommission(account.getCommission() + commission(contract, instrument));
            }
        } else if (state.compareToIgnoreCase("closed") == 0) {
            account.setCloseProfit(account.getCloseProfit() + closeProfit(contract, instrument));
            account.setCommission(account.getCommission() + commission(contract, instrument));
        }
    }

    private static void settleContract0(Account account, Contract contract, Double settlementPrice, Instrument instrument) throws SettlementException {
        var state = contract.getState();
        if (state.compareToIgnoreCase("opening") == 0) {
            account.setOpeningMargin(account.getOpeningMargin() + margin(contract, instrument));
            account.setOpeningCommission(account.getOpeningCommission() + commission(contract, instrument));
        } else if (state.compareToIgnoreCase("closing") == 0) {
            account.setMargin(account.getMargin() + margin(contract, instrument));
            account.setClosingCommission(account.getCommission() + commission(contract, instrument));
            account.setPositionProfit(account.getPositionProfit() + positionProfit(settlementPrice, contract, instrument));
        }
    }

    private static Double margin(Contract contract, Instrument instrument) {
        return computeRatio(contract.getPrice(), instrument.getMultiple(), instrument.getAmountMargin(), instrument.getVolumeMargin());
    }

    private static Double positionProfit(Double settlementPrice, Contract contract, Instrument instrument) throws SettlementException {
        if (contract.getClosePrice() != null && contract.getClosePrice() != 0) {
            throw new SettlementException("Open(closing) contract(" + contract.getContractId() + ") has close price.");
        }
        return profit(contract, settlementPrice, instrument.getMultiple());
    }

    private static Double commission(Contract contract, Instrument instrument) {
        return computeRatio(contract.getPrice(), instrument.getMultiple(), instrument.getAmountCommission(), instrument.getVolumeCommission());
    }

    private static Double closeProfit(Contract contract, Instrument instrument) throws SettlementException {
        if (contract.getClosePrice() == null || contract.getClosePrice() == 0) {
            throw new SettlementException("Closed contract(" + contract.getContractId() + ") has no close price.");
        }
        return profit(contract, contract.getClosePrice(), instrument.getMultiple());
    }

    private static Double profit(Contract contract, Double closePrice, Double multiple) throws SettlementException {
        var direction = contract.getDirection();
        var price = contract.getPrice();
        if (direction.compareToIgnoreCase("buy") == 0) {
            return (closePrice - price) * multiple;
        } else if (direction.compareToIgnoreCase("sell") == 0) {
            return (price - closePrice) * multiple;
        } else {
            throw new SettlementException("Contract(" + contract.getContractId() + ") has invalid direction(" + direction + ").");
        }
    }

    private static void resetAccount(Account account, String tradingDay) {
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
        account.setSettleTime(Utils.datetime());
    }

    static HashMap<String, UserSnapshot> users(Set<User> users, Set<Account> accounts, Set<Contract> contracts) throws SettlementException {
        var snapshots = new HashMap<String, UserSnapshot>();
        var userIterator = users.iterator();
        while (userIterator.hasNext()) {
            var user = userIterator.next();
            if (snapshots.containsKey(user.getUserId())) {
                throw new SettlementException("Duplicated user(" + user.getUserId() + ").");
            }
            var snapshot = snapshots.computeIfAbsent(user.getUserId(), key -> new UserSnapshot());
            snapshot.setUser(user);
            accounts(snapshot, accounts, contracts);
            userIterator.remove();
        }
        return snapshots;
    }

    static void accounts(UserSnapshot userSnapshot, Set<Account> accounts, Set<Contract> contracts) throws SettlementException {
        var user = userSnapshot.getUser();
        /* Find user's account. */
        var accountIterator = accounts.iterator();
        while (accountIterator.hasNext()) {
            var account = accountIterator.next();
            if (account.getUserId().equals(user.getUserId())) {
                if (userSnapshot.getAccount() != null) {
                    throw new SettlementException("Duplicated account(" + account.getAccountId() + "/" + userSnapshot.getAccount().getAccountId() + ") for user(" + user.getUserId() + ").");
                }
                userSnapshot.setAccount(account);
            }
            accountIterator.remove();
        }
        contracts(userSnapshot, contracts);
    }

    static void contracts(UserSnapshot userSnapshot, Set<Contract> contracts) {
        /* Find user's contracts. */
        var contractIterator = contracts.iterator();
        while (contractIterator.hasNext()) {
            var contract = contractIterator.next();
            if (contract.getUserId().equals(userSnapshot.getUser().getUserId())) {
                userSnapshot.contracts().computeIfAbsent(contract.getInstrumentId(), key -> new HashSet<Contract>()).add(contract);
            }
            contractIterator.remove();
        }
    }

    private static Map<String, Tick> tickMap(Collection<Tick> ticks) {
        var mapping = new HashMap<String, Tick>();
        ticks.forEach(tick -> {
            mapping.put(tick.getInstrumentId(), tick);
        });
        return mapping;
    }

    private static Map<String, Instrument> instrumentMap(Collection<Instrument> instruments) {
        var mapping = new HashMap<String, Instrument>();
        instruments.forEach(instrument -> {
            mapping.put(instrument.getInstrumentId(), instrument);
        });
        return mapping;
    }

    private static Tick getTick(String instrumentId, Map<String, Tick> ticks) throws SettlementException {
        var tick = ticks.get(instrumentId);
        if (tick == null) {
            throw new SettlementException("Tick(" + instrumentId + ") not found.");
        }
        return tick;
    }

    private static Instrument getInstrument(String instrumentId, Map<String, Instrument> instruments) throws SettlementException {
        var instrument = instruments.get(instrumentId);
        if (instrument == null) {
            throw new SettlementException("Instrument(" + instrumentId + ") not found.");
        }
        return instrument;
    }
}
