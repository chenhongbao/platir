package io.platir.core.internal;

import io.platir.queries.QueriesImpl;
import io.platir.queries.Utils;
import io.platir.service.Account;
import io.platir.service.Contract;
import io.platir.service.DataQueryException;
import io.platir.service.Queries;
import io.platir.service.ServiceConstants;
import io.platir.service.api.ApiConstants;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 *
 * @author Chen Hongbao
 */
public class SettlementTest {

    private Queries queries;

    @BeforeEach
    public void setUp() throws Exception {
        Utils.delete(Utils.applicationDirectory());
        queries = new QueriesImpl();
        prepareSchema();
    }

    @AfterEach
    public void tearDown() throws Exception {
        queries = null;
        Utils.delete(Utils.applicationDirectory());
    }

    @Test
    public void testSettle() throws Exception {
        new Settlement(queries).settle();
        var accounts = queries.selectAccounts();
        var iterator = accounts.iterator();
        var account = iterator.next();
        var account2 = iterator.next();
        /* Check account. */
        assertEquals(20000D, account.getYdBalance(), 0.01, "YdBalance not matched.");
        assertEquals(0D, account.getPositionProfit(), 0.01, "Position profit wrong.");
        assertEquals(-100D, account.getCloseProfit(), 0.01, "Close profit wrong.");
        assertEquals(10305D, account.getMargin(), 0.01, "Margin wrong.");
        assertEquals(0D, account.getOpeningMargin(), 0.01, "Opening margin wrong.");
        assertEquals(0D, account.getOpeningCommission(), 0.01, "Opening commission wrong.");
        assertEquals(8.4D, account.getCommission(), 0.01, "Commission wrong.");
        assertEquals(0D, account.getClosingCommission(), 0.01, "Closing commission wrong.");
        assertEquals(19891.6D, account.getBalance(), 0.01, "Balance wrong.");
        assertEquals(9586.6D, account.getAvailable(), 0.01, "Available wrong.");
        /* Check contracts. */
        int openC2201Count = 0;
        int openC2205Count = 0;
        var contracts = queries.selectContracts().stream().filter(contract -> {
            return contract.getUserId().equals(account.getUserId());
        }).collect(Collectors.toSet());
        for (var contract : contracts) {
            if (contract.getInstrumentId().equals("c2201")) {
                assertEquals(2575D, contract.getPrice(), "Contract price wrong.");
                ++openC2201Count;
            }
            if (contract.getInstrumentId().equals("c2205")) {
                assertEquals(2570D, contract.getPrice(), "Contract price wrong.");
                ++openC2205Count;
            }
        }
        assertEquals(4, contracts.size(), "Contract count wrong.");
        assertEquals(3, openC2201Count, "Contract count wrong.");
        assertEquals(1, openC2205Count, "Contract count wrong.");
        /* Check two accounts are the same. */
        account.setAccountId(account2.getAccountId());
        account.setUserId(account2.getUserId());
        account.setSettleTime(account2.getSettleTime());
        Utils.beanEquals(Account.class, account, account2);
    }

    @Test
    public void testSettleInDay() throws Exception {
        Set<UserSnapshot> userBackup = createUserSnapshot();
        Set<UserSnapshot> users = createUserSnapshot();
        var iterator = users.iterator();
        var snapshot1 = iterator.next();
        var snapshot2 = iterator.next();
        /* Settlement in day. */
        SettlementFacilities.settleInDay(snapshot1, queries.selectTradingDay().getDay(), queries.selectTicks(), queries.selectInstruments());
        SettlementFacilities.settleInDay(snapshot2, queries.selectTradingDay().getDay(), queries.selectTicks(), queries.selectInstruments());
        /* Check account. */
        var account = snapshot1.getAccount();
        assertEquals(20000D, account.getYdBalance(), 0.01, "YdBalance not matched.");
        assertEquals(100D, account.getPositionProfit(), 0.01, "Position profit wrong.");
        assertEquals(-100D, account.getCloseProfit(), 0.01, "Close profit wrong.");
        assertEquals(10305D, account.getMargin(), 0.01, "Margin wrong.");
        assertEquals(5160D, account.getOpeningMargin(), 0.01, "Opening margin wrong.");
        assertEquals(2.4D, account.getOpeningCommission(), 0.01, "Opening commission wrong.");
        assertEquals(8.4D, account.getCommission(), 0.01, "Commission wrong.");
        assertEquals(1.2D, account.getClosingCommission(), 0.01, "Closing commission wrong.");
        assertEquals(19991.6D, account.getBalance(), 0.01, "Balance wrong.");
        assertEquals(4523D, account.getAvailable(), 0.001D, "Available wrong.");
        /* Check contracts are not changed. */
        for (var snapshot : userBackup) {
            boolean found = false;
            for (var sn2 : users) {
                if (snapshot.getUser().getUserId().equals(sn2.getUser().getUserId())) {
                    found = true;
                    snapshot.contracts().keySet().forEach(instrumentId -> {
                        var contracts = snapshot.contracts().get(instrumentId);
                        assertTrue(Utils.collectionEquals(Contract.class, contracts, sn2.contracts().get(instrumentId)), "Contracts are unexpectedly change!");
                    });
                    break;
                }
            }
            if (!found) {
                fail("User(" + snapshot.getUser().getUserId() + ") not found in settled snapshot.");
            }
        }
        /* Check two accounts are the same. */
        account.setAccountId(snapshot2.getAccount().getAccountId());
        account.setUserId(snapshot2.getAccount().getUserId());
        account.setSettleTime(snapshot2.getAccount().getSettleTime());
        Utils.beanEquals(Account.class, account, snapshot2.getAccount());
    }

    private void prepareSchema() throws Exception {
        var tradingDay = Utils.date();
        var preDay = Integer.toString(Integer.parseInt(tradingDay) - 1);
        /* Set up information. */
        addInstrument("c2201", "DCE", 0D, 1.2D, 0.1D, 0D, 10D, Utils.datetime());
        addInstrument("c2205", "DCE", 0D, 1.2D, 0.1D, 0D, 10D, Utils.datetime());
        addTick("c2201", 2580D, 2575D);
        addTick("c2205", 2575D, 2570D);
        addTradingDay(tradingDay);
        /* Add user1. */
        addUser("user1");
        addAccount("account1", "user1", 20000.0D);
        addContract("user1.contract1", "user1", "c2201", 2580.0D, null, ServiceConstants.FLAG_CONTRACT_OPEN, ApiConstants.FLAG_BUY, preDay, null, null);
        addContract("user1.contract2", "user1", "c2201", 2585.0D, null, ServiceConstants.FLAG_CONTRACT_OPEN, ApiConstants.FLAG_BUY, tradingDay, null, null);
        addContract("user1.contract3", "user1", "c2205", 2575.0D, null, ServiceConstants.FLAG_CONTRACT_OPEN, ApiConstants.FLAG_SELL, tradingDay, null, null);
        addContract("user1.contract4", "user1", "c2201", 2565.0D, null, ServiceConstants.FLAG_CONTRACT_OPENING, ApiConstants.FLAG_BUY, null, null, null);
        addContract("user1.contract5", "user1", "c2205", 2595.0D, null, ServiceConstants.FLAG_CONTRACT_OPENING, ApiConstants.FLAG_SELL, null, null, null);
        addContract("user1.contract6", "user1", "c2201", 2565.0D, null, ServiceConstants.FLAG_CONTRACT_CLOSING, ApiConstants.FLAG_BUY, tradingDay, null, null);
        addContract("user1.contract7", "user1", "c2205", 2565.0D, 2580.D, ServiceConstants.FLAG_CONTRACT_CLOSED, ApiConstants.FLAG_BUY, tradingDay, null, null);
        addContract("user1.contract8", "user1", "c2205", 2565.0D, 2590.D, ServiceConstants.FLAG_CONTRACT_CLOSED, ApiConstants.FLAG_SELL, tradingDay, null, null);
        /* Add user2. */
        addUser("user2");
        addAccount("account2", "user2", 20000.0D);
        addContract("user2.contract1", "user2", "c2201", 2580.0D, null, ServiceConstants.FLAG_CONTRACT_OPEN, ApiConstants.FLAG_BUY, preDay, null, null);
        addContract("user2.contract2", "user2", "c2201", 2585.0D, null, ServiceConstants.FLAG_CONTRACT_OPEN, ApiConstants.FLAG_BUY, tradingDay, null, null);
        addContract("user2.contract3", "user2", "c2205", 2575.0D, null, ServiceConstants.FLAG_CONTRACT_OPEN, ApiConstants.FLAG_SELL, tradingDay, null, null);
        addContract("user2.contract4", "user2", "c2201", 2565.0D, null, ServiceConstants.FLAG_CONTRACT_OPENING, ApiConstants.FLAG_BUY, null, null, null);
        addContract("user2.contract5", "user2", "c2205", 2595.0D, null, ServiceConstants.FLAG_CONTRACT_OPENING, ApiConstants.FLAG_SELL, null, null, null);
        addContract("user2.contract6", "user2", "c2201", 2565.0D, null, ServiceConstants.FLAG_CONTRACT_CLOSING, ApiConstants.FLAG_BUY, tradingDay, null, null);
        addContract("user2.contract7", "user2", "c2205", 2565.0D, 2580.D, ServiceConstants.FLAG_CONTRACT_CLOSED, ApiConstants.FLAG_BUY, tradingDay, null, null);
        addContract("user2.contract8", "user2", "c2205", 2565.0D, 2590.D, ServiceConstants.FLAG_CONTRACT_CLOSED, ApiConstants.FLAG_SELL, tradingDay, null, null);
        /* Add two identical users so it checks whether settlements are the same. */
    }

    private void addAccount(String accountId, String userId, Double balance) throws DataQueryException {
        var account = queries.getFactory().newAccount();
        account.setAccountId(accountId);
        account.setUserId(userId);
        account.setBalance(balance);
        queries.insert(account);
    }

    private void addTradingDay(String tradingDay) throws DataQueryException {
        var day = queries.getFactory().newTradingDay();
        day.setDay(tradingDay);
        day.setUpdateTime(Utils.datetime());
        queries.insert(day);
    }

    private void addUser(String userId) throws DataQueryException {
        var user = queries.getFactory().newUser();
        user.setUserId(userId);
        user.setCreateTime(Utils.datetime());
        queries.insert(user);
    }

    private void addContract(String contractId, String userId, String instrumentId, Double price, Double closePrice, String state, String direction, String openTradingDay, String openTime, String settlementTradingDay) throws DataQueryException {
        var contract = queries.getFactory().newContract();
        contract.setClosePrice(closePrice);
        contract.setContractId(contractId);
        contract.setDirection(direction);
        contract.setInstrumentId(instrumentId);
        contract.setOpenTime(openTime);
        contract.setOpenTradingDay(openTradingDay);
        contract.setPrice(price);
        contract.setState(state);
        contract.setUserId(userId);
        contract.setSettlementTradingDay(settlementTradingDay);
        queries.insert(contract);
    }

    private void addInstrument(String instrumentId, String exchangeId, Double amountCommission, Double volumeCommission, Double amountMargin, Double volumeMargin, Double multiple, String updateTime) throws Exception {
        var instrument = queries.getFactory().newInstrument();
        instrument.setAmountCommission(amountCommission);
        instrument.setAmountMargin(amountMargin);
        instrument.setExchangeId(exchangeId);
        instrument.setInstrumentId(instrumentId);
        instrument.setMultiple(multiple);
        instrument.setUpdateTime(updateTime);
        instrument.setVolumeCommission(volumeCommission);
        instrument.setVolumeMargin(volumeMargin);
        queries.insert(instrument);
    }

    private void addTick(String instrumentId, Double lastPrice, Double settlementPrice) throws Exception {
        var tick = queries.getFactory().newTick();
        tick.setInstrumentId(instrumentId);
        tick.setLastPrice(lastPrice);
        tick.setSettlementPrice(settlementPrice);
        queries.insert(tick);
    }

    private Set<UserSnapshot> createUserSnapshot() throws DataQueryException {
        var accounts = queries.selectAccounts();
        var users = queries.selectUsers();
        var contracts = queries.selectContracts();
        Set<UserSnapshot> snapshots = new HashSet<>();
        for (var user : users) {
            var snapshot = new UserSnapshot();
            snapshot.setUser(user);
            accounts.stream().filter(account -> (account.getUserId().equals(user.getUserId()))).forEachOrdered(account -> {
                snapshot.setAccount(account);
            });
            if (snapshot.getAccount() == null) {
                throw new DataQueryException("No account for user(" + user.getUserId() + ").");
            }
            contracts.stream().filter(contract -> (contract.getUserId().equals(user.getUserId()))).forEachOrdered(contract -> {
                var instrumentContracts = snapshot.contracts().computeIfAbsent(contract.getInstrumentId(), key -> new HashSet<>());
                instrumentContracts.add(contract);
            });
            snapshots.add(snapshot);
        }
        return snapshots;
    }
}
