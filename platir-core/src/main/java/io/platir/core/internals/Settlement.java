package io.platir.core.internals;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import io.platir.core.PlatirSystem;
import io.platir.core.RuntimeSnapshot;
import io.platir.core.SettlementException;
import io.platir.service.Account;
import io.platir.service.Contract;
import io.platir.service.Instrument;
import io.platir.service.StrategyProfile;
import io.platir.service.Tick;
import io.platir.service.api.Queries;

public class Settlement extends SettlementFacilities {

	private final Gson g;
	private final Queries qry;
	private final RuntimeSnapshot snapshot = new RuntimeSnapshot();
	private final Set<Tick> ticks = new HashSet<>();
	private final Set<Instrument> instruments = new HashSet<>();
	private File before;
	private File after;

	public Settlement(Queries queries) {
		super();
		g = setupGson();
		qry = queries;
	}

	private Gson setupGson() {
		return new GsonBuilder().serializeNulls().create();
	}

	public void settle() throws SQLException, SettlementException {
		prepareDirs();
		snapshot();
		backup(before, snapshot);
		computeSettlement();
		backup(after, snapshot);
		roll();
	}

	private void roll() throws SQLException {
		clearTables();
		pushTables();
	}

	private void pushTables() throws SQLException {
		qry.insert(snapshot.accounts().toArray(new Account[1]));
		qry.insert(snapshot.contracts().toArray(new Contract[1]));
		/* insert the updated strategy profiles */
		removeSomeStrategies();
		qry.insert(snapshot.strategyProfiles().toArray(new StrategyProfile[1]));
	}
	
	private void removeSomeStrategies() {
		/* remove strategies that have been removed */
		var stgItr = snapshot.strategyProfiles().iterator();
		while (stgItr.hasNext()) {
			var s = stgItr.next();
			if (s.getState().compareToIgnoreCase("removed") == 0) {
				stgItr.remove();
			}
		}
	}

	private void clearTables() throws SQLException {
		/* ticks are set before settlement */
		qry.clearTicks();
		/* write settled information */
		qry.clearAccounts();
		qry.clearContracts();
		/* clear obsolete data */
		qry.clearTrades();
		qry.clearOrders();
		qry.clearTransactions();
		/* clear strategies for update */
		qry.clearStrategies();
	}

	private void computeSettlement() throws SettlementException, SQLException {
		var users = users(snapshot.users(), snapshot.accounts(), snapshot.contracts());
		requireEmpty(snapshot.accounts(), "Some accounts have no owner.");
		requireEmpty(snapshot.contracts(), "Some contracts have no owner.");
		var tradingDay = qry.selectTradingDay().getTradingDay();
		for (var u : users.values()) {
			settle(u, tradingDay, ticks, instruments);
			push0(u);
		}
		/* clear snapshot so backup the settled data */
		snapshot.orders().clear();
		snapshot.trades().clear();
		snapshot.transactions().clear();
	}

	private void push0(UserSnapshot u) {
		snapshot.accounts().add(u.account);
		u.contracts.values().forEach(cs -> {
			snapshot.contracts().addAll(cs);
		});

	}

	private void backup(File target, Object data) {
		try (FileWriter fw = new FileWriter(target)) {
			fw.write(g.toJson(data));
			fw.flush();
		} catch (IOException e) {
			PlatirSystem.err.write(
					"Can't write snapshot data to file \'" + target.getAbsolutePath() + "\'. " + e.getMessage(), e);
		}

	}

	private void prepareDirs() {
		var dir = PlatirSystem.dir(Paths.get(PlatirSystem.cwd().toString(), PlatirSystem.date()));
		before = PlatirSystem.file(Paths.get(dir.toString(), "before_settlement.json"));
		after = PlatirSystem.file(Paths.get(dir.toString(), "settled.json"));
	}

	private void snapshot() throws SQLException {
		snapshot.accounts().addAll(qry.selectAccounts());
		snapshot.contracts().addAll(qry.selectContracts());
		snapshot.instruments().addAll(qry.selectInstruments());
		snapshot.orders().addAll(qry.selectOrders());
		snapshot.strategyProfiles().addAll(qry.selectStrategyProfiles());
		snapshot.trades().addAll(qry.selectTrades());
		snapshot.transactions().addAll(qry.selectTransactions());
		snapshot.users().addAll(qry.selectUsers());
		ticks.addAll(qry.selectTicks());
		instruments.addAll(qry.selectInstruments());
	}

	private void requireEmpty(Collection<?> container, String message) throws SettlementException {
		if (!container.isEmpty()) {
			throw new SettlementException(message);
		}
	}
}
