package io.platir.core.internal;

import java.io.File;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import io.platir.core.SettlementException;
import io.platir.service.Account;
import io.platir.service.Contract;
import io.platir.service.Instrument;
import io.platir.service.StrategyProfile;
import io.platir.service.Tick;
import io.platir.service.DataQueryException;
import io.platir.service.Queries;

public class Settlement {

    private final Queries queries;
    private final RuntimeSnapshot snapshot = new RuntimeSnapshot();
    private final Set<Tick> ticks = new HashSet<>();
    private final Set<Instrument> instruments = new HashSet<>();
    private File before;
    private File after;

    public Settlement(Queries queries) {
        this.queries = queries;
    }

    public void settle() throws DataQueryException, SettlementException {
        prepareDirs();
        queries.backup(before);
        snapshot();
        computeSettlement();
        roll();
        queries.backup(after);
    }

    private void roll() throws DataQueryException {
        clearTables();
        pushTables();
    }

    private void pushTables() throws DataQueryException {
        queries.insert(snapshot.accounts().toArray(new Account[1]));
        queries.insert(snapshot.contracts().toArray(new Contract[1]));
        /* Insert the updated strategy profiles. */
        removeSomeStrategies();
        queries.insert(snapshot.strategyProfiles().toArray(new StrategyProfile[1]));
    }

    private void removeSomeStrategies() {
        /* Remove strategies that was scheduled to remove. */
        var profileIterator = snapshot.strategyProfiles().iterator();
        while (profileIterator.hasNext()) {
            var profile = profileIterator.next();
            if (profile.getState().compareToIgnoreCase("removed") == 0) {
                profileIterator.remove();
            }
        }
    }

    private void clearTables() throws DataQueryException {
        /* Ticks are set before settlement. */
        queries.clearTicks();
        /* Write settled information. */
        queries.clearAccounts();
        queries.clearContracts();
        /* Clear obsolete data. */
        queries.clearTrades();
        queries.clearOrders();
        queries.clearTransactions();
        /* Clear strategies for update. */
        queries.clearStrategies();
    }

    private void computeSettlement() throws SettlementException, DataQueryException {
        java.util.HashMap<java.lang.String, io.platir.core.internal.UserSnapshot> users = SettlementFacilities.users(snapshot.users(), snapshot.accounts(), snapshot.contracts());
        requireEmpty(snapshot.accounts(), "Some accounts have no owner.");
        requireEmpty(snapshot.contracts(), "Some contracts have no owner.");
        var tradingDay = queries.selectTradingDay().getTradingDay();
        for (io.platir.core.internal.UserSnapshot user : users.values()) {
            SettlementFacilities.settle(user, tradingDay, ticks, instruments);
            push0(user);
        }
        /* clear snapshot so backup the settled data */
        snapshot.orders().clear();
        snapshot.trades().clear();
        snapshot.transactions().clear();
    }

    private void push0(UserSnapshot userSnapshot) {
        snapshot.accounts().add(userSnapshot.getAccount());
        userSnapshot.contracts().values().forEach(contracts -> {
            snapshot.contracts().addAll(contracts);
        });

    }

    private void prepareDirs() {
        var dir = Utils.dir(Paths.get(Utils.cwd().toString(), Utils.date()));
        before = Utils.file(Paths.get(dir.toString(), "before_settlement.json"));
        after = Utils.file(Paths.get(dir.toString(), "settled.json"));
    }

    private void snapshot() throws DataQueryException {
        snapshot.accounts().addAll(queries.selectAccounts());
        snapshot.contracts().addAll(queries.selectContracts());
        snapshot.instruments().addAll(queries.selectInstruments());
        snapshot.orders().addAll(queries.selectOrders());
        snapshot.strategyProfiles().addAll(queries.selectStrategyProfiles());
        snapshot.trades().addAll(queries.selectTrades());
        snapshot.transactions().addAll(queries.selectTransactions());
        snapshot.users().addAll(queries.selectUsers());
        ticks.addAll(queries.selectTicks());
        instruments.addAll(queries.selectInstruments());
    }

    private void requireEmpty(Collection<?> container, String message) throws SettlementException {
        if (!container.isEmpty()) {
            throw new SettlementException(message);
        }
    }
}
