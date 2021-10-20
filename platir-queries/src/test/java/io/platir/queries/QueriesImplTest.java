package io.platir.queries;

import io.platir.service.Account;
import io.platir.service.Contract;
import io.platir.service.Factory;
import io.platir.service.Instrument;
import io.platir.service.Queries;
import io.platir.service.RiskNotice;
import io.platir.service.StrategyProfile;
import io.platir.service.Tick;
import io.platir.service.Trade;
import io.platir.service.TradingDay;
import io.platir.service.Transaction;
import io.platir.service.User;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.TestMethodOrder;

/**
 *
 * @author Chen Hongbao
 */
@TestMethodOrder(OrderAnnotation.class)
public class QueriesImplTest {

    private Queries queries;

    public QueriesImplTest() {
    }

    @BeforeAll
    public void setup() throws IOException {
        queries = new QueriesImpl();
        Utils.delete(Paths.get(Utils.cwd().toString(), "Schema"), false);
    }

    @AfterAll
    public void destroy() throws IOException {
        Utils.delete(Paths.get(Utils.cwd().toString(), "Schema"), false);
        queries = null;
    }

    @Test
    @Order(1)
    @DisplayName("Test initializing empty tables.")
    public void testInitializeEmptyTable() throws Exception {
        queries.initialize();
        assertTrue(queries.selectAccounts().isEmpty());
        assertTrue(queries.selectContracts().isEmpty());
        assertTrue(queries.selectInstruments().isEmpty());
        assertTrue(queries.selectOrders().isEmpty());
        assertTrue(queries.selectStrategyProfiles().isEmpty());
        assertTrue(queries.selectTicks().isEmpty());
        assertTrue(queries.selectTrades().isEmpty());
        assertTrue(queries.selectTransactions().isEmpty());
        assertTrue(queries.selectUsers().isEmpty());
        assertTrue(queries.selectTradingDay() == null);
    }

    @Test
    @Order(999)
    @DisplayName("Test destroying tables.")
    public void testDestroy() throws Exception {
        queries.destroy();
        testInitializeEmptyTable();
    }

    @Test
    @Order(998)
    @DisplayName("Test backing up and restoring backup tables.")
    public void testBackupRestore() throws Exception {
        var file = Paths.get(Utils.backupDirectory().toString(), "unit-test-backup.json");
        /* Step 1: Back up schema. */
        var oldSchema = queries.backup(file.toFile());
        /* Step 2: Make change to schema. */
        testInsert_AccountArr();
        testInsert_ContractArr();
        testInsert_InstrumentArr();
        testInsert_StrategyProfileArr();
        testInsert_TickArr();
        testInsert_TradingDay();
        testInsert_UserArr();
        testClearTrades();
        testClearOrders();
        testClearTransactions();
        /* Risk notice is special because it is not backup. */
        testInsert_RiskNoticeArr();
        /* Step 3: Restore schema. */
        queries.restore(file.toFile());
        /* Verify restored tables. */
        assertTrue(TestUtils.collectionEquals(Account.class, oldSchema.getAccounts(), queries.selectAccounts()), "Account restore failed.");
        assertTrue(TestUtils.collectionEquals(Contract.class, oldSchema.getContracts(), queries.selectContracts()), "Contract restore failed.");
        assertTrue(TestUtils.collectionEquals(Instrument.class, oldSchema.getInstruments(), queries.selectInstruments()), "Instrument restore failed.");
        assertTrue(WrapOrder.collectionEquals(oldSchema.getOrders(), queries.selectOrders()), "Order restore failed.");
        assertTrue(TestUtils.collectionEquals(StrategyProfile.class, oldSchema.getStrategyProfiles(), queries.selectStrategyProfiles()), "StrategyProfile restore failed.");
        assertTrue(TestUtils.collectionEquals(Tick.class, oldSchema.getTicks(), queries.selectTicks()), "Tick restore failed.");
        assertTrue(TestUtils.collectionEquals(Trade.class, oldSchema.getTrades(), queries.selectTrades()), "Trade restore failed.");
        assertTrue(TestUtils.collectionEquals(Transaction.class, oldSchema.getTransactions(), queries.selectTransactions()), "Transaction restore failed.");
        assertTrue(TestUtils.collectionEquals(User.class, oldSchema.getUsers(), queries.selectUsers()), "User restore failed.");
        assertEquals(oldSchema.getTradingDay(), queries.selectTradingDay(), "TradingDay restore failed.");
    }

    @Test
    public void testInsert_OrderArr() throws Exception {
        System.out.println("insert");
        WrapOrder order = null;
        QueriesImpl instance = new QueriesImpl();
        instance.insert(order);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of insert method, of class QueriesImpl.
     */
    @org.junit.jupiter.api.Test
    public void testInsert_TradingDay() throws Exception {
        System.out.println("insert");
        TradingDay day = null;
        QueriesImpl instance = new QueriesImpl();
        instance.insert(day);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of insert method, of class QueriesImpl.
     */
    @org.junit.jupiter.api.Test
    public void testInsert_AccountArr() throws Exception {
        System.out.println("insert");
        Account[] accounts = null;
        QueriesImpl instance = new QueriesImpl();
        instance.insert(accounts);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of insert method, of class QueriesImpl.
     */
    @org.junit.jupiter.api.Test
    public void testInsert_TickArr() throws Exception {
        System.out.println("insert");
        Tick[] ticks = null;
        QueriesImpl instance = new QueriesImpl();
        instance.insert(ticks);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of insert method, of class QueriesImpl.
     */
    @org.junit.jupiter.api.Test
    public void testInsert_TransactionArr() throws Exception {
        System.out.println("insert");
        Transaction[] transactions = null;
        QueriesImpl instance = new QueriesImpl();
        instance.insert(transactions);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of insert method, of class QueriesImpl.
     */
    @org.junit.jupiter.api.Test
    public void testInsert_TradeArr() throws Exception {
        System.out.println("insert");
        Trade[] trades = null;
        QueriesImpl instance = new QueriesImpl();
        instance.insert(trades);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of insert method, of class QueriesImpl.
     */
    @org.junit.jupiter.api.Test
    public void testInsert_ContractArr() throws Exception {
        System.out.println("insert");
        Contract[] contracts = null;
        QueriesImpl instance = new QueriesImpl();
        instance.insert(contracts);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of insert method, of class QueriesImpl.
     */
    @org.junit.jupiter.api.Test
    public void testInsert_UserArr() throws Exception {
        System.out.println("insert");
        User[] users = null;
        QueriesImpl instance = new QueriesImpl();
        instance.insert(users);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of insert method, of class QueriesImpl.
     */
    @org.junit.jupiter.api.Test
    public void testInsert_StrategyProfileArr() throws Exception {
        System.out.println("insert");
        StrategyProfile[] profiles = null;
        QueriesImpl instance = new QueriesImpl();
        instance.insert(profiles);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of insert method, of class QueriesImpl.
     */
    @org.junit.jupiter.api.Test
    public void testInsert_InstrumentArr() throws Exception {
        System.out.println("insert");
        Instrument[] instruments = null;
        QueriesImpl instance = new QueriesImpl();
        instance.insert(instruments);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of insert method, of class QueriesImpl.
     */
    @org.junit.jupiter.api.Test
    public void testInsert_RiskNoticeArr() throws Exception {
        System.out.println("insert");
        RiskNotice[] notices = null;
        QueriesImpl instance = new QueriesImpl();
        instance.insert(notices);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of update method, of class QueriesImpl.
     */
    @org.junit.jupiter.api.Test
    public void testUpdate_AccountArr() throws Exception {
        System.out.println("update");
        Account[] accounts = null;
        QueriesImpl instance = new QueriesImpl();
        instance.update(accounts);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of update method, of class QueriesImpl.
     */
    @org.junit.jupiter.api.Test
    public void testUpdate_ContractArr() throws Exception {
        System.out.println("update");
        Contract[] contracts = null;
        QueriesImpl instance = new QueriesImpl();
        instance.update(contracts);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of update method, of class QueriesImpl.
     */
    @org.junit.jupiter.api.Test
    public void testUpdate_TransactionArr() throws Exception {
        System.out.println("update");
        Transaction[] transactions = null;
        QueriesImpl instance = new QueriesImpl();
        instance.update(transactions);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of update method, of class QueriesImpl.
     */
    @org.junit.jupiter.api.Test
    public void testUpdate_InstrumentArr() throws Exception {
        System.out.println("update");
        Instrument[] instruments = null;
        QueriesImpl instance = new QueriesImpl();
        instance.update(instruments);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of update method, of class QueriesImpl.
     */
    @org.junit.jupiter.api.Test
    public void testUpdate_UserArr() throws Exception {
        System.out.println("update");
        User[] users = null;
        QueriesImpl instance = new QueriesImpl();
        instance.update(users);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of update method, of class QueriesImpl.
     */
    @org.junit.jupiter.api.Test
    public void testUpdate_StrategyProfileArr() throws Exception {
        System.out.println("update");
        StrategyProfile[] profiles = null;
        QueriesImpl instance = new QueriesImpl();
        instance.update(profiles);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of updateTradingDay method, of class QueriesImpl.
     */
    @org.junit.jupiter.api.Test
    public void testUpdateTradingDay() throws Exception {
        System.out.println("updateTradingDay");
        TradingDay day = null;
        QueriesImpl instance = new QueriesImpl();
        instance.updateTradingDay(day);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of clearAccounts method, of class QueriesImpl.
     */
    @org.junit.jupiter.api.Test
    public void testClearAccounts() throws Exception {
        System.out.println("clearAccounts");
        QueriesImpl instance = new QueriesImpl();
        instance.clearAccounts();
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of clearContracts method, of class QueriesImpl.
     */
    @org.junit.jupiter.api.Test
    public void testClearContracts() throws Exception {
        System.out.println("clearContracts");
        QueriesImpl instance = new QueriesImpl();
        instance.clearContracts();
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of clearOrders method, of class QueriesImpl.
     */
    @org.junit.jupiter.api.Test
    public void testClearOrders() throws Exception {
        System.out.println("clearOrders");
        QueriesImpl instance = new QueriesImpl();
        instance.clearOrders();
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of clearTrades method, of class QueriesImpl.
     */
    @org.junit.jupiter.api.Test
    public void testClearTrades() throws Exception {
        System.out.println("clearTrades");
        QueriesImpl instance = new QueriesImpl();
        instance.clearTrades();
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of clearTransactions method, of class QueriesImpl.
     */
    @org.junit.jupiter.api.Test
    public void testClearTransactions() throws Exception {
        System.out.println("clearTransactions");
        QueriesImpl instance = new QueriesImpl();
        instance.clearTransactions();
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of clearTicks method, of class QueriesImpl.
     */
    @org.junit.jupiter.api.Test
    public void testClearTicks() throws Exception {
        System.out.println("clearTicks");
        QueriesImpl instance = new QueriesImpl();
        instance.clearTicks();
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of clearStrategies method, of class QueriesImpl.
     */
    @org.junit.jupiter.api.Test
    public void testClearStrategies() throws Exception {
        System.out.println("clearStrategies");
        QueriesImpl instance = new QueriesImpl();
        instance.clearStrategies();
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of clearRiskNotices method, of class QueriesImpl.
     */
    @org.junit.jupiter.api.Test
    public void testClearRiskNotices() throws Exception {
        System.out.println("clearRiskNotices");
        QueriesImpl instance = new QueriesImpl();
        instance.clearRiskNotices();
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of selectTradingDay method, of class QueriesImpl.
     */
    @org.junit.jupiter.api.Test
    public void testSelectTradingDay() throws Exception {
        System.out.println("selectTradingDay");
        QueriesImpl instance = new QueriesImpl();
        TradingDay expResult = null;
        TradingDay result = instance.selectTradingDay();
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of selectAccounts method, of class QueriesImpl.
     */
    @org.junit.jupiter.api.Test
    public void testSelectAccounts() throws Exception {
        System.out.println("selectAccounts");
        QueriesImpl instance = new QueriesImpl();
        Set<Account> expResult = null;
        Set<Account> result = instance.selectAccounts();
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of selectContracts method, of class QueriesImpl.
     */
    @org.junit.jupiter.api.Test
    public void testSelectContracts() throws Exception {
        System.out.println("selectContracts");
        QueriesImpl instance = new QueriesImpl();
        Set<Contract> expResult = null;
        Set<Contract> result = instance.selectContracts();
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of selectInstruments method, of class QueriesImpl.
     */
    @org.junit.jupiter.api.Test
    public void testSelectInstruments() throws Exception {
        System.out.println("selectInstruments");
        QueriesImpl instance = new QueriesImpl();
        Set<Instrument> expResult = null;
        Set<Instrument> result = instance.selectInstruments();
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of selectStrategyProfiles method, of class QueriesImpl.
     */
    @org.junit.jupiter.api.Test
    public void testSelectStrategyProfiles() throws Exception {
        System.out.println("selectStrategyProfiles");
        QueriesImpl instance = new QueriesImpl();
        Set<StrategyProfile> expResult = null;
        Set<StrategyProfile> result = instance.selectStrategyProfiles();
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of selectTrades method, of class QueriesImpl.
     */
    @org.junit.jupiter.api.Test
    public void testSelectTrades() throws Exception {
        System.out.println("selectTrades");
        QueriesImpl instance = new QueriesImpl();
        Set<Trade> expResult = null;
        Set<Trade> result = instance.selectTrades();
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of selectTransactions method, of class QueriesImpl.
     */
    @org.junit.jupiter.api.Test
    public void testSelectTransactions() throws Exception {
        System.out.println("selectTransactions");
        QueriesImpl instance = new QueriesImpl();
        Set<Transaction> expResult = null;
        Set<Transaction> result = instance.selectTransactions();
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of selectUsers method, of class QueriesImpl.
     */
    @org.junit.jupiter.api.Test
    public void testSelectUsers() throws Exception {
        System.out.println("selectUsers");
        QueriesImpl instance = new QueriesImpl();
        Set<User> expResult = null;
        Set<User> result = instance.selectUsers();
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of selectTicks method, of class QueriesImpl.
     */
    @org.junit.jupiter.api.Test
    public void testSelectTicks() throws Exception {
        System.out.println("selectTicks");
        QueriesImpl instance = new QueriesImpl();
        Set<Tick> expResult = null;
        Set<Tick> result = instance.selectTicks();
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of getFactory method, of class QueriesImpl.
     */
    @org.junit.jupiter.api.Test
    public void testGetFactory() {
        System.out.println("getFactory");
        QueriesImpl instance = new QueriesImpl();
        Factory expResult = null;
        Factory result = instance.getFactory();
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

}
