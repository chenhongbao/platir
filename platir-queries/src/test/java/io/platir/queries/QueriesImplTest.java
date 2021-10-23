package io.platir.queries;

import io.platir.service.Account;
import io.platir.service.Contract;
import io.platir.service.DataQueryException;
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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.BeforeEach;
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

    @BeforeEach
    public void setup() throws IOException {
        Utils.delete(Utils.applicationDirectory());
        queries = new QueriesImpl();
    }

    @AfterEach
    public void destroy() throws IOException {
        queries = null;
        Utils.delete(Utils.applicationDirectory());
    }

    @Test
    @Order(1)
    @DisplayName("Test initializing empty tables.")
    public void testInitializeEmptyTable() throws Exception {
        queries.initialize();
        checkEmptySchema();
    }

    @Test
    @Order(999)
    @DisplayName("Test backing up and restoring backup tables.")
    public void testBackupRestore() throws Exception {
        var path = Paths.get(Utils.backupDirectory().toString(), Utils.date());
        Utils.dir(path);
        /* Step 1: Back up schema. */
        testAccount();
        testContract();
        testInstrument();
        testStrategyProfile();
        testTick();
        var oldSchema = queries.backup(path);
        /* Step 2: Make change to schema. */
        testTradingDay();
        testUser();
        testTrade();
        testOrder();
        testTransaction();
        /* Risk notice is special because it is not backup. */
        testRiskNotice();
        /* Step 3: Restore schema. */
        queries.restore(path);
        /* Verify restored tables. */
        assertTrue(Utils.collectionEquals(Account.class, oldSchema.getAccounts(), queries.selectAccounts()), "Account restore failed.");
        assertTrue(Utils.collectionEquals(Contract.class, oldSchema.getContracts(), queries.selectContracts()), "Contract restore failed.");
        assertTrue(Utils.collectionEquals(Instrument.class, oldSchema.getInstruments(), queries.selectInstruments()), "Instrument restore failed.");
        assertTrue(WrapOrder.collectionEquals(oldSchema.getOrders(), queries.selectOrders()), "Order restore failed.");
        assertTrue(Utils.collectionEquals(StrategyProfile.class, oldSchema.getStrategyProfiles(), queries.selectStrategyProfiles()), "StrategyProfile restore failed.");
        assertTrue(Utils.collectionEquals(Tick.class, oldSchema.getTicks(), queries.selectTicks()), "Tick restore failed.");
        assertTrue(Utils.collectionEquals(Trade.class, oldSchema.getTrades(), queries.selectTrades()), "Trade restore failed.");
        assertTrue(Utils.collectionEquals(Transaction.class, oldSchema.getTransactions(), queries.selectTransactions()), "Transaction restore failed.");
        assertTrue(Utils.collectionEquals(User.class, oldSchema.getUsers(), queries.selectUsers()), "User restore failed.");
        assertTrue(Utils.beanEquals(TradingDay.class, oldSchema.getTradingDay(), queries.selectTradingDay()), "TradingDay restore failed.");
    }

    @Test
    @Order(100)
    @DisplayName("Test insert/select Order.")
    public void testOrder() throws Exception {
        assertTrue(WrapOrder.testOrder(queries), "Order restore failed.");
    }

    @Test
    @Order(101)
    @DisplayName("Test insert/select TradingDay.")
    public void testTradingDay() throws Exception {
        /* Step 1: Insert TradingDay. */
        TradingDay day = queries.getFactory().newTradingDay();
        day.setDay(Utils.date());
        day.setUpdateTime(Utils.datetime());
        queries.insert(day);
        /* Step 2: Check insertion succeeds. */
        assertTrue(Utils.beanEquals(TradingDay.class, day, queries.selectTradingDay()));
        /* Step 3; Load schema files. */
        var anotherQueries = new QueriesImpl();
        anotherQueries.initialize();
        /* Step 4: Check loaded schema against runtime. */
        assertTrue(Utils.beanEquals(TradingDay.class, anotherQueries.selectTradingDay(), queries.selectTradingDay()), "TradingDay restore failed.");
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
    public void testContract() throws Exception {
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
    
    @Test
    @Order(118)
    @DisplayName("Test remove contract.")
    public void testRemoveContract() throws Exception {
        var contract = queries.getFactory().newContract();
        contract.setContractId(UUID.randomUUID().toString());
        queries.insert(contract);
        /* Remove the contract. */
        queries.remove(contract);
        /* Check selected contracts empty. */
        var contracts = queries.selectContracts();
        assertTrue(contracts.isEmpty(), "Contract remove failed.");
    }

    private void checkEmptySchema() throws DataQueryException {
        assertTrue(queries.selectAccounts().isEmpty());
        assertTrue(queries.selectContracts().isEmpty());
        assertTrue(queries.selectInstruments().isEmpty());
        assertTrue(queries.selectOrders().isEmpty());
        assertTrue(queries.selectStrategyProfiles().isEmpty());
        assertTrue(queries.selectTicks().isEmpty());
        assertTrue(queries.selectTrades().isEmpty());
        assertTrue(queries.selectTransactions().isEmpty());
        assertTrue(queries.selectUsers().isEmpty());
        assertTrue(Utils.beanEquals(TradingDay.class, queries.selectTradingDay(), new TradingDayImpl()));
    }

    private <T> void testInsertSelect(Class<T> clazz) throws Exception {
        String name = null;
        /* Step 1: Create new instance by reflecting on Factory. */
        Method factory;
        try {
            name = "new" + clazz.getSimpleName();
            factory = queries.getFactory().getClass().getMethod(name);
        } catch (NoSuchMethodException exception) {
            fail("Can't find factory method " + name + ", " + exception.getMessage());
            return;
        }
        /* Step 2: Create some instances to be inserted. */
        var keySetterName = "set" + clazz.getSimpleName() + "Id";
        Method keySetter;
        try {
            keySetter = clazz.getMethod(keySetterName, String.class);
        } catch (NoSuchMethodException exception) {
            fail("Can't find key setter " + keySetterName + ", " + exception.getMessage());
            return;
        }
        Set<T> items = new HashSet<>();
        var totalInsertionCount = Utils.positiveRandomInteger() % 100 + 1;
        while (totalInsertionCount-- > 0) {
            var item = factory.invoke(queries.getFactory());
            if (item != null) {
                /* Item needs a key. */
                keySetter.invoke(item, UUID.randomUUID().toString());
                setOtherIds(clazz, item);
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
        inserter.invoke(queries, (Object) toArray(clazz, items));
        /* Step 4: Get the inserted items for checking. */
        Method selector;
        try {
            name = "select" + clazz.getSimpleName() + "s";
            selector = queries.getClass().getMethod(name);
        } catch (NoSuchMethodException exception) {
            fail("Can't find selector method " + name + ", " + exception.getMessage());
            return;
        }
        /* Step 5: Check inserted items. */
        assertTrue(Utils.collectionEquals(clazz, items, (Set<T>) selector.invoke(queries)), clazz.getSimpleName() + " runtime insertion failed.");
        /* Step 6: Check schema file udpate success. */
        var anotherQueries = new QueriesImpl();
        anotherQueries.initialize();
        assertTrue(Utils.collectionEquals(clazz, (Set<T>) selector.invoke(anotherQueries), (Set<T>) selector.invoke(queries)), clazz.getSimpleName() + " loading schema failed.");
    }

    private <T> void testUpdate(Class<T> clazz) throws Exception {
        String name = null;
        /* Step 1: Select items to be updated. */
        Method selector;
        try {
            name = "select" + clazz.getSimpleName() + "s";
            selector = queries.getClass().getMethod(name);
        } catch (NoSuchMethodException ignored) {
            String name2 = "select" + clazz.getSimpleName();
            try {
                selector = queries.getClass().getMethod(name2);
            } catch (NoSuchMethodException exception) {
                fail("Can't find selector method " + name + " or " + name2 + ", " + exception.getMessage());
                return;
            }
        }
        Set<T> items = selectSet(clazz, selector);
        /* Step 2: Choose a field to update by reflecting on its setter. */
        Method setter = null;
        var keySetter = "set" + clazz.getSimpleName() + "Id";
        for (var method : clazz.getMethods()) {
            if (method.getName().equals(keySetter)) {
                continue;
            }
            var params = method.getParameterTypes();
            if (params.length != 1 || params[0] != String.class) {
                continue;
            }
            setter = method;
            break;
        }
        if (setter == null) {
            fail("No setter(String) found in " + clazz.getSimpleName() + ".");
            return;
        }
        /* Update field. */
        for (var item : items) {
            setter.invoke(item, UUID.randomUUID().toString());
        }
        /* Step 3: Update items into schema. */
        Method updater;
        try {
            name = "update";
            updater = queries.getClass().getMethod(name, Array.newInstance(clazz, 1).getClass());
            updater.invoke(queries, (Object) toArray(clazz, items));
        } catch (NoSuchMethodException exception) {
            fail("Can't find updater method " + name + ", " + exception.getMessage(), exception);
            return;
        }
        /* Step 4: Check updated items. */
        assertTrue(Utils.collectionEquals(clazz, items, selectSet(clazz, selector)), clazz.getSimpleName() + " runtime update failed.");
        /* Step 5: Check schema file udpate success. */
        var anotherQueries = new QueriesImpl();
        anotherQueries.initialize();
        assertTrue(Utils.collectionEquals(clazz, selectSet(clazz, selector, anotherQueries), selectSet(clazz, selector)), clazz.getSimpleName() + " loading schema failed.");
    }

    private <T> Set<T> selectSet(Class<T> clazz, Method selector) throws Exception {
        return selectSet(clazz, selector, queries);
    }

    private <T> Set<T> selectSet(Class<T> clazz, Method selector, Object base) throws Exception {
        Set<T> items;
        if (clazz == TradingDay.class) {
            items = new HashSet<>();
            items.add((T) selector.invoke(base));
        } else {
            items = (Set<T>) selector.invoke(base);
        }
        return items;
    }

    private <T> T[] toArray(Class<T> clazz, Set<T> items) {
        var array = (T[]) Array.newInstance(clazz, items.size());
        int index = 0;
        for (var item : items) {
            array[index++] = item;
        }
        return array;
    }

    private <T> void setOtherIds(Class<T> clazz, Object item) throws Exception {
        Method m;
        if (clazz == Tick.class) {
            m = clazz.getMethod("setInstrumentId", String.class);
            m.invoke(item, UUID.randomUUID().toString());
        }
        if (clazz == StrategyProfile.class) {
            m = clazz.getMethod("setStrategyId", String.class);
            m.invoke(item, UUID.randomUUID().toString());
        }
    }
}
