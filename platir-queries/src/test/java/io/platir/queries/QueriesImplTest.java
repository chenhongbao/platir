package io.platir.queries;

import io.platir.service.Account;
import io.platir.service.Contract;
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
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
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
        testAccount();
        testInsert_ContractArr();
        testInstrument();
        testStrategyProfile();
        testTick();
        testTradingDay();
        testUser();
        testTrade();
        testOrder();
        testTransaction();
        /* Risk notice is special because it is not backup. */
        testRiskNotice();
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
        assertTrue(TestUtils.beanEquals(TradingDay.class, oldSchema.getTradingDay(), queries.selectTradingDay()), "TradingDay restore failed.");
    }

    @Test
    @Order(100)
    @DisplayName("Test insert/select Order.")
    public void testOrder() throws Exception {
        WrapOrder.testOrder(queries);
    }

    @Test
    @Order(101)
    @DisplayName("Test insert/select TradingDay.")
    public void testTradingDay() throws Exception {
        /* Step 1: Insert TradingDay. */
        TradingDay day = queries.getFactory().newTradingDay();
        day.setTradingDay(Utils.date());
        day.setUpdateTime(Utils.datetime());
        queries.insert(day);
        /* Step 2: Check insertion succeeds. */
        assertTrue(TestUtils.beanEquals(TradingDay.class, day, queries.selectTradingDay()));
        /* Step 3; Load schema files. */
        var anotherQueries = new QueriesImpl();
        anotherQueries.initialize();
        /* Step 4: Check loaded schema against runtime. */
        assertTrue(TestUtils.beanEquals(TradingDay.class, anotherQueries.selectTradingDay(), queries.selectTradingDay()), "TradingDay restore failed.");
    }

    @Test
    @Order(102)
    @DisplayName("Test insert/select Account.")
    public void testAccount() throws Exception {
        testInsertSelect(Account.class);
    }

    @Test
    @Order(103)
    @DisplayName("Test insert/select Tick.")
    public void testTick() throws Exception {
        testInsertSelect(Tick.class);
    }

    @Test
    @Order(104)
    @DisplayName("Test insert/select Transaction.")
    public void testTransaction() throws Exception {
        testInsertSelect(Transaction.class);
    }

    @Test
    @Order(105)
    @DisplayName("Test insert/select Trade.")
    public void testTrade() throws Exception {
        testInsertSelect(Trade.class);
    }

    @Test
    @Order(106)
    @DisplayName("Test insert/select Contract.")
    public void testInsert_ContractArr() throws Exception {
        testInsertSelect(Contract.class);
    }

    @Test
    @Order(107)
    @DisplayName("Test insert/select User.")
    public void testUser() throws Exception {
        testInsertSelect(User.class);
    }

    @Test
    @Order(108)
    @DisplayName("Test insert/select StrategyProfile.")
    public void testStrategyProfile() throws Exception {
        testInsertSelect(StrategyProfile.class);
    }

    @Test
    @Order(109)
    @DisplayName("Test insert/select Instrument.")
    public void testInstrument() throws Exception {
        testInsertSelect(Instrument.class);
    }

    @Test
    @Order(110)
    @DisplayName("Test insert/select RiskNotice.")
    public void testRiskNotice() throws Exception {
        testInsertSelect(RiskNotice.class);
    }

    @Test
    @Order(111)
    @DisplayName("Test update Account.")
    public void testUpdateAccount() throws Exception {
        testUpdate(Account.class);
    }

    @Test
    @Order(112)
    @DisplayName("Test update Contract.")
    public void testUpdateContract() throws Exception {
        testUpdate(Contract.class);
    }

    @Test
    @Order(113)
    @DisplayName("Test update Transaction.")
    public void testUpdateTransaction() throws Exception {
        testUpdate(Transaction.class);
    }

    @Test
    @Order(114)
    @DisplayName("Test update Instrument.")
    public void testUpdateInstrument() throws Exception {
        testUpdate(Instrument.class);
    }

    @Test
    @Order(115)
    @DisplayName("Test update User.")
    public void testUpdateUser() throws Exception {
        testUpdate(User.class);
    }

    @Test
    @Order(116)
    @DisplayName("Test update StrategyProfile.")
    public void testUpdateStrategyProfile() throws Exception {
        testUpdate(StrategyProfile.class);
    }

    @Test
    @Order(117)
    @DisplayName("Test update TradingDay.")
    public void testUpdateTradingDay() throws Exception {
        testUpdate(TradingDay.class);
    }

    private <T> void testInsertSelect(Class<T> clazz) throws Exception {
        String name = null;
        /* Step 1: Create new instance by reflecting on Factory. */
        Method factory;
        try {
            name = "new" + clazz.getCanonicalName();
            factory = queries.getFactory().getClass().getMethod(name);
        } catch (NoSuchMethodException exception) {
            fail("Can't find factory method " + name + ", " + exception.getMessage());
            return;
        }
        /* Step 2: Create some instances to be inserted. */
        Set<T> items = new HashSet<>();
        var totalInsertionCount = TestUtils.randomInteger();
        while (totalInsertionCount-- > 0) {
            var item = factory.invoke(queries.getFactory());
            if (item != null) {
                items.add((T) item);
            } else {
                fail("Factory method " + name + " returns null.");
            }
        }
        /* Step 3: Insert items. */
        Method inserter;
        try {
            name = "insert";
            inserter = queries.getClass().getMethod(name, Array.newInstance(clazz, 1).getClass());
        } catch (NoSuchMethodException exception) {
            fail("Can't find inserter method " + name + ", " + exception.getMessage());
            return;
        }
        inserter.invoke(queries, items.toArray());
        /* Step 4: Get the inserted items for checking. */
        Method selector;
        try {
            name = "select" + Account.class.getCanonicalName() + "s";
            selector = queries.getClass().getMethod(name);
        } catch (NoSuchMethodException exception) {
            fail("Can't find selector method " + name + ", " + exception.getMessage());
            return;
        }
        /* Step 5: Check inserted items. */
        assertTrue(TestUtils.collectionEquals(clazz, items, (Set<T>) selector.invoke(queries)), clazz.getCanonicalName() + " runtime insertion failed.");
        /* Step 6: Check schema file udpate success. */
        var anotherQueries = new QueriesImpl();
        anotherQueries.initialize();
        assertTrue(TestUtils.collectionEquals(clazz, (Set<T>) selector.invoke(anotherQueries), (Set<T>) selector.invoke(queries)), clazz.getCanonicalName() + " loading schema failed.");
    }

    private <T> void testUpdate(Class<T> clazz) throws Exception {
        String name = null;
        /* Step 1: Select items to be updated. */
        Method selector;
        try {
            name = "select" + Account.class.getCanonicalName() + "s";
            selector = queries.getClass().getMethod(name);
        } catch (NoSuchMethodException exception) {
            fail("Can't find selector method " + name + ", " + exception.getMessage());
            return;
        }
        var items = (Set<T>) selector.invoke(queries);
        /* Step 2: Choose a field to update by reflecting on its setter. */
        Method setter = null;
        var keySetter = "set" + clazz.getCanonicalName() + "Id";
        for (var method : clazz.getMethods()) {
            if (method.getName().equals(keySetter)) {
                continue;
            }
            var params = method.getParameterTypes();
            if (params.length != 1 || params[1] != String.class) {
                continue;
            }
            setter = method;
            break;
        }
        if (setter == null) {
            fail("No setter(String) found in " + clazz.getCanonicalName() + ".");
            return;
        }
        /* Update field. */
        for (var item : items) {
            setter.invoke(item, UUID.randomUUID().toString());
        }
        /* Step 3: Update items into schema. */
        Method updater;
        try {
            name = "update" + clazz.getCanonicalName() + "s";
            updater = queries.getClass().getMethod(name, Array.newInstance(clazz, 1).getClass());
            updater.invoke(queries, items);
        } catch (NoSuchMethodException exception) {
            fail("Can't find updater method " + name + " in " + clazz.getCanonicalName() + ", " + exception.getMessage(), exception);
            return;
        }
        /* Step 4: Check updated items. */
        assertTrue(TestUtils.collectionEquals(clazz, items, (Set<T>) selector.invoke(queries)), clazz.getCanonicalName() + " runtime update failed.");
        /* Step 5: Check schema file udpate success. */
        var anotherQueries = new QueriesImpl();
        anotherQueries.initialize();
        assertTrue(TestUtils.collectionEquals(clazz, (Set<T>) selector.invoke(anotherQueries), (Set<T>) selector.invoke(queries)), clazz.getCanonicalName() + " loading schema failed.");
    }
}
