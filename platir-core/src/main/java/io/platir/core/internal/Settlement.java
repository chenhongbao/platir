package io.platir.core.internal;

import io.platir.queries.Utils;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import io.platir.core.SettlementException;
import io.platir.service.Contract;
import io.platir.service.Instrument;
import io.platir.service.StrategyProfile;
import io.platir.service.Tick;
import io.platir.service.DataQueryException;
import io.platir.service.Queries;
import io.platir.service.ServiceConstants;
import java.nio.file.Path;

public class Settlement {

    private final Queries queries;
    private final Set<Tick> ticks = new HashSet<>();
    private final Set<Instrument> instruments = new HashSet<>();
    private Path before;
    private Path after;

    public Settlement(Queries queries) {
        this.queries = queries;
    }

    public void settle() throws DataQueryException, SettlementException {
        prepare();
        queries.backup(before);
        var snapshot = createSnapshot();
        var users = computeSettlement(snapshot);
        rollQueries(snapshot, users);
        queries.backup(after);
    }

    private void rollQueries(RuntimeSnapshot runtimeSnapshot, Set<UserSnapshot> userSnapshots) throws DataQueryException {
        clearSchema();
        resetSchema(runtimeSnapshot, userSnapshots);
    }

    private void resetSchema(RuntimeSnapshot runtimeSnapshot, Set<UserSnapshot> userSnapshots) throws DataQueryException {
        for (var snapshot : userSnapshots) {
            queries.insert(snapshot.getAccount());
            for (var contracts : snapshot.contracts().values()) {
                queries.insert(contracts.toArray(new Contract[1]));
            }
        }
        removeSomeStrategies(runtimeSnapshot);
        if (!runtimeSnapshot.strategyProfiles().isEmpty()) {
            queries.insert(runtimeSnapshot.strategyProfiles().toArray(new StrategyProfile[1]));
        }
    }

    private void removeSomeStrategies(RuntimeSnapshot snapshot) {
        /* Remove strategies that was scheduled to remove. */
        var profileIterator = snapshot.strategyProfiles().iterator();
        while (profileIterator.hasNext()) {
            var profile = profileIterator.next();
            if (profile.getState().compareToIgnoreCase(ServiceConstants.FLAG_STRATEGY_REMOVED) == 0) {
                profileIterator.remove();
            }
        }
    }

    private void clearSchema() throws DataQueryException {
        /* Ticks are set before settlement every time. */
        queries.clearTicks();
        /* Clear for rewriting settled information. */
        queries.clearAccounts();
        queries.clearContracts();
        /* Clear obsolete data. */
        queries.clearTrades();
        queries.clearOrders();
        queries.clearTransactions();
        /* Clear strategies for update. */
        queries.clearStrategies();
    }

    private Set<UserSnapshot> computeSettlement(RuntimeSnapshot snapshot) throws SettlementException, DataQueryException {
        var users = SettlementFacilities.users(snapshot.users(), snapshot.accounts(), snapshot.contracts());
        requireEmpty(snapshot.accounts(), "Some accounts have no owner.");
        requireEmpty(snapshot.contracts(), "Some contracts have no owner.");
        for (var user : users.values()) {
            SettlementFacilities.settle(user, snapshot.getTradingDay().getDay(), ticks, instruments);
        }
        return new HashSet<>(users.values());
    }

    private void prepare() throws DataQueryException {
        var dir = Utils.dir(Paths.get(Utils.backupDirectory().toString(), Utils.date()));
        before = Utils.dir(Paths.get(dir.toString(), "ThisDay"));
        after = Utils.dir(Paths.get(dir.toString(), "NextDay"));
        ticks.addAll(queries.selectTicks());
        instruments.addAll(queries.selectInstruments());
    }

    private RuntimeSnapshot createSnapshot() throws DataQueryException {
        var snapshot = new RuntimeSnapshot();
        snapshot.accounts().addAll(queries.selectAccounts());
        snapshot.contracts().addAll(queries.selectContracts());
        snapshot.instruments().addAll(queries.selectInstruments());
        snapshot.orders().addAll(queries.selectOrders());
        snapshot.strategyProfiles().addAll(queries.selectStrategyProfiles());
        snapshot.trades().addAll(queries.selectTrades());
        snapshot.transactions().addAll(queries.selectTransactions());
        snapshot.users().addAll(queries.selectUsers());
        snapshot.setTradingDay(queries.selectTradingDay());
        chooseProperTradingDay(snapshot);
        return snapshot;
    }

    private void requireEmpty(Collection<?> container, String message) throws SettlementException {
        if (!container.isEmpty()) {
            throw new SettlementException(message);
        }
    }

    private void chooseProperTradingDay(RuntimeSnapshot snapshot) {
        /* Always choose the latest date because trading day may not be updated. */
        var properTradingDay = Utils.maxDate(snapshot.getTradingDay().getDay(), Utils.date());
        snapshot.getTradingDay().setDay(properTradingDay);
    }
}
