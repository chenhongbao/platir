package io.platir.queries;

import com.google.gson.Gson;
import io.platir.service.Account;
import io.platir.service.Contract;
import io.platir.service.Instrument;
import io.platir.service.Order;
import io.platir.service.RiskNotice;
import io.platir.service.StrategyProfile;
import io.platir.service.Tick;
import io.platir.service.Trade;
import io.platir.service.TradingDay;
import io.platir.service.Transaction;
import io.platir.service.User;
import io.platir.service.DataQueryException;
import io.platir.service.Queries;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

/**
 *
 * @author Chen Hongbao
 */
public class QueriesImpl implements Queries {

    private final AtomicReference<TradingDay> tradingDay = new AtomicReference<>();
    private final Map<String, Account> accountTable = new HashMap<>();
    private final Map<String, Tick> tickTable = new HashMap<>();
    private final Map<String, Transaction> transactionTable = new HashMap<>();
    private final Map<String, Order> orderTable = new HashMap<>();
    private final Map<String, Trade> tradeTable = new HashMap<>();
    private final Map<String, Contract> contractTable = new HashMap<>();
    private final Map<String, User> userTable = new HashMap<>();
    private final Map<String, StrategyProfile> profileTable = new HashMap<>();
    private final Map<String, Instrument> instrumentTable = new HashMap<>();
    private final List<RiskNotice> riskNoticeTable = new LinkedList<>();

    private final Gson g;

    public QueriesImpl() {
        g = new Gson().newBuilder().serializeNulls().setPrettyPrinting().create();
    }

    @Override
    public void initialize() throws DataQueryException {
        readTable(Account.class).rows.forEach(item -> {
            accountTable.put(item.getAccountId(), item);
        });
        readTable(Tick.class).rows.forEach(item -> {
            tickTable.put(item.getInstrumentId(), item);
        });
        readTable(Transaction.class).rows.forEach(item -> {
            transactionTable.put(item.getTransactionId(), item);
        });
        readTable(Order.class).rows.forEach(item -> {
            orderTable.put(item.getOrderId(), item);
        });
        readTable(Trade.class).rows.forEach(item -> {
            tradeTable.put(item.getTradeId(), item);
        });
        readTable(Contract.class).rows.forEach(item -> {
            contractTable.put(item.getContractId(), item);
        });
        readTable(User.class).rows.forEach(item -> {
            userTable.put(item.getUserId(), item);
        });
        readTable(StrategyProfile.class).rows.forEach(item -> {
            profileTable.put(item.getStrategyId(), item);
        });
        readTable(Instrument.class).rows.forEach(item -> {
            instrumentTable.put(item.getInstrumentId(), item);
        });
    }

    @Override
    public void destroy() throws DataQueryException {
        tradingDay.set(null);
        instrumentTable.clear();
        userTable.clear();
        clearAccounts();
        clearContracts();
        clearOrders();
        clearStrategies();
        clearTicks();
        clearTrades();
        clearTransactions();
    }

    @Override
    public void backup(File target) {
        try {
            var schema = new QuerySchema();
            schema.backupTime = Utils.datetime();
            schema.tradingDay = tradingDay.get();
            schema.accounts.addAll(selectAccounts());
            schema.ticks.addAll(selectTicks());
            schema.transactions.addAll(selectTransactions());
            schema.orders.addAll(selectOrders());
            schema.trades.addAll(selectTrades());
            schema.contracts.addAll(selectContracts());
            schema.users.addAll(selectUsers());
            schema.strategyProfiles.addAll(selectStrategyProfiles());
            schema.instruments.addAll(selectInstruments());
            writeJson(target, schema);
        } catch (DataQueryException ex) {
            Utils.err.write("Can't backup schema: " + ex.getMessage(), ex);
        }
    }

    @Override
    public void insert(TradingDay day) throws DataQueryException {
        if (day == null) {
            throw new DataQueryException("Insert null object.");
        }
        tradingDay.set(day);
        /* persist */
        writeTradingDay();
    }

    @Override
    public void insert(Account... accounts) throws DataQueryException {
        synchronized (accountTable) {
            for (var account : accounts) {
                if (account.getAccountId() == null) {
                    throw new DataQueryException("Account has a null ID.");
                }
                ensureNotExists(accountTable, account.getAccountId());
                accountTable.put(account.getAccountId(), account);
            }
            /* persist account table */
            writeTable(Account.class, accountTable.values());
        }
    }

    @Override
    public void insert(Tick... ticks) throws DataQueryException {
        synchronized (tickTable) {
            for (var tick : ticks) {
                if (tick.getInstrumentId() == null) {
                    throw new DataQueryException("Tick has a null instrument ID.");
                }
                ensureNotExists(tickTable, tick.getInstrumentId());
                tickTable.put(tick.getInstrumentId(), tick);
            }
            writeTable(Tick.class, tickTable.values());
        }
    }

    @Override
    public void insert(Transaction... transactions) throws DataQueryException {
        synchronized (transactionTable) {
            for (var transaction : transactions) {
                if (transaction.getTransactionId() == null) {
                    throw new DataQueryException("Transaction has a null ID.");
                }
                ensureNotExists(transactionTable, transaction.getTransactionId());
                transactionTable.put(transaction.getTransactionId(), transaction);
            }
            writeTable(Transaction.class, transactionTable.values());
        }
    }

    @Override
    public void insert(Order... orders) throws DataQueryException {
        synchronized (orderTable) {
            for (var order : orders) {
                if (order.getOrderId() == null) {
                    throw new DataQueryException("Order has a null ID.");
                }
                ensureNotExists(orderTable, order.getOrderId());
                orderTable.put(order.getOrderId(), order);
            }
            writeTable(Order.class, orderTable.values());
        }
    }

    @Override
    public void insert(Trade... trades) throws DataQueryException {
        synchronized (tradeTable) {
            for (var trade : trades) {
                if (trade.getTradeId() == null) {
                    throw new DataQueryException("Trade has a null ID.");
                }
                ensureNotExists(tradeTable, trade.getTradeId());
                tradeTable.put(trade.getTradeId(), trade);
            }
            writeTable(Trade.class, tradeTable.values());
        }
    }

    @Override
    public void insert(Contract... contracts) throws DataQueryException {
        synchronized (contractTable) {
            for (var contract : contracts) {
                if (contract.getContractId() == null) {
                    throw new DataQueryException("Contract has a null ID.");
                }
                ensureNotExists(contractTable, contract.getInstrumentId());
                contractTable.put(contract.getContractId(), contract);
            }
            writeTable(Contract.class, contractTable.values());
        }
    }

    @Override
    public void insert(User... users) throws DataQueryException {
        synchronized (userTable) {
            for (var user : users) {
                if (user.getUserId() == null) {
                    throw new DataQueryException("User has a null ID.");
                }
                ensureNotExists(userTable, user.getUserId());
                userTable.put(user.getUserId(), user);
            }
            writeTable(User.class, userTable.values());
        }
    }

    @Override
    public void insert(StrategyProfile... profiles) throws DataQueryException {
        synchronized (profileTable) {
            for (var profile : profiles) {
                if (profile.getStrategyId() == null) {
                    throw new DataQueryException("Strategy has a null ID.");
                }
                ensureNotExists(profileTable, profile.getStrategyId());
                profileTable.put(profile.getStrategyId(), profile);
            }
            writeTable(StrategyProfile.class, profileTable.values());
        }
    }

    @Override
    public void insert(Instrument... instruments) throws DataQueryException {
        synchronized (instrumentTable) {
            for (var instrument : instruments) {
                if (instrument.getInstrumentId() == null) {
                    throw new DataQueryException("Instrument has a null ID.");
                }
                ensureNotExists(instrumentTable, instrument.getInstrumentId());
                instrumentTable.put(instrument.getInstrumentId(), instrument);
            }
            writeTable(Instrument.class, instrumentTable.values());
        }
    }

    @Override
    public void insert(RiskNotice... notices) throws DataQueryException {
        synchronized (riskNoticeTable) {
            riskNoticeTable.addAll(Arrays.asList(notices));
        }
    }

    @Override
    public void update(Account... accounts) throws DataQueryException {
        synchronized (accountTable) {
            for (var account : accounts) {
                if (account.getAccountId() == null) {
                    throw new DataQueryException("Account has a null ID.");
                }
                ensureExists(accountTable, account.getAccountId());
                accountTable.put(account.getAccountId(), account);
            }
            writeTable(Account.class, accountTable.values());
        }
    }

    @Override
    public void update(Contract... contracts) throws DataQueryException {
        synchronized (contractTable) {
            for (var contract : contracts) {
                if (contract.getContractId() == null) {
                    throw new DataQueryException("Contract has a null ID.");
                }
                ensureExists(contractTable, contract);
                contractTable.put(contract.getContractId(), contract);
            }
            writeTable(Contract.class, contractTable.values());
        }
    }

    @Override
    public void update(Transaction... transactions) throws DataQueryException {
        synchronized (transactionTable) {
            for (var transaction : transactions) {
                if (transaction.getTransactionId() == null) {
                    throw new DataQueryException("Transaction has a null ID.");
                }
                ensureExists(transactionTable, transaction.getTransactionId());
                transactionTable.put(transaction.getTransactionId(), transaction);
            }
            writeTable(Transaction.class, transactionTable.values());
        }
    }

    @Override
    public void update(Instrument... instruments) throws DataQueryException {
        synchronized (instrumentTable) {
            for (var instrument : instruments) {
                if (instrument.getInstrumentId() == null) {
                    throw new DataQueryException("Instrument has a null ID.");
                }
                ensureExists(instrumentTable, instrument.getInstrumentId());
                instrumentTable.put(instrument.getInstrumentId(), instrument);
            }
            writeTable(Instrument.class, instrumentTable.values());
        }
    }

    @Override
    public void update(User... users) throws DataQueryException {
        synchronized (userTable) {
            for (var user : users) {
                if (user.getUserId() == null) {
                    throw new DataQueryException("User has a null ID.");
                }
                ensureExists(userTable, user.getUserId());
                userTable.put(user.getUserId(), user);
            }
            writeTable(User.class, userTable.values());
        }
    }

    @Override
    public void update(StrategyProfile... profiles) throws DataQueryException {
        synchronized (profileTable) {
            for (var profile : profiles) {
                if (profile.getStrategyId() == null) {
                    throw new DataQueryException("Strategy profile has a null ID.");
                }
                ensureExists(profileTable, profile.getStrategyId());
                profileTable.put(profile.getStrategyId(), profile);
            }
            writeTable(StrategyProfile.class, profileTable.values());
        }
    }

    @Override
    public void updateTradingDay(TradingDay day) throws DataQueryException {
        if (day == null) {
            throw new DataQueryException("Insert a null object.");
        }
        tradingDay.set(day);
        writeTradingDay();
    }

    @Override
    public void clearAccounts() throws DataQueryException {
        synchronized (accountTable) {
            accountTable.clear();
            writeTable(Account.class, accountTable.values());
        }
    }

    @Override
    public void clearContracts() throws DataQueryException {
        synchronized (contractTable) {
            contractTable.clear();
            writeTable(Contract.class, contractTable.values());
        }
    }

    @Override
    public void clearOrders() throws DataQueryException {
        synchronized (orderTable) {
            orderTable.clear();
            writeTable(Order.class, orderTable.values());
        }
    }

    @Override
    public void clearTrades() throws DataQueryException {
        synchronized (tradeTable) {
            tradeTable.clear();
            writeTable(Trade.class, tradeTable.values());
        }
    }

    @Override
    public void clearTransactions() throws DataQueryException {
        synchronized (transactionTable) {
            transactionTable.clear();
            writeTable(Transaction.class, transactionTable.values());
        }
    }

    @Override
    public void clearTicks() throws DataQueryException {
        synchronized (tickTable) {
            tickTable.clear();
            writeTable(Tick.class, tickTable.values());
        }
    }

    @Override
    public void clearStrategies() throws DataQueryException {
        synchronized (profileTable) {
            profileTable.clear();
            writeTable(StrategyProfile.class, profileTable.values());
        }
    }

    @Override
    public void clearRiskNotices() throws DataQueryException {
        synchronized (riskNoticeTable) {
            riskNoticeTable.clear();
        }
    }

    @Override
    public TradingDay selectTradingDay() throws DataQueryException {
        var day = ObjectFactory.newTradingDay();
        day.setTradingDay(tradingDay.get().getTradingDay());
        day.setUpdateTime(tradingDay.get().getUpdateTime());
        return day;
    }

    @Override
    public Set<Account> selectAccounts() throws DataQueryException {
        synchronized (accountTable) {
            var accounts = new HashSet<Account>();
            accountTable.values().stream().map(item -> {
                var account = ObjectFactory.newAccount();
                account.setAccountId(item.getAccountId());
                account.setAvailable(item.getAvailable());
                account.setBalance(item.getBalance());
                account.setCloseProfit(item.getCloseProfit());
                account.setClosingCommission(item.getClosingCommission());
                account.setCommission(item.getCommission());
                account.setMargin(item.getMargin());
                account.setOpeningCommission(item.getOpeningCommission());
                account.setOpeningMargin(item.getOpeningMargin());
                account.setPositionProfit(item.getPositionProfit());
                account.setSettleTime(item.getSettleTime());
                account.setTradingDay(item.getTradingDay());
                account.setUserId(item.getUserId());
                account.setYdBalance(item.getYdBalance());
                return account;
            }).forEachOrdered(account -> {
                accounts.add(account);
            });
            return accounts;
        }
    }

    @Override
    public Set<Contract> selectContracts() throws DataQueryException {
        synchronized (contractTable) {
            var contracts = new HashSet<Contract>();
            contractTable.values().stream().map(item -> {
                var contract = ObjectFactory.newContract();
                contract.setClosePrice(item.getClosePrice());
                contract.setContractId(item.getContractId());
                contract.setDirection(item.getDirection());
                contract.setInstrumentId(item.getInstrumentId());
                contract.setOpenTime(item.getOpenTime());
                contract.setOpenTradingDay(item.getOpenTradingDay());
                contract.setPrice(item.getPrice());
                contract.setState(item.getState());
                contract.setUserId(item.getUserId());
                return contract;
            }).forEachOrdered(contract -> {
                contracts.add(contract);
            });
            return contracts;
        }
    }

    @Override
    public Set<Instrument> selectInstruments() throws DataQueryException {
        synchronized (instrumentTable) {
            var instruments = new HashSet<Instrument>();
            instrumentTable.values().stream().map(item -> {
                var instrument = ObjectFactory.newInstrument();
                instrument.setAmountCommission(item.getAmountCommission());
                instrument.setAmountMargin(item.getAmountMargin());
                instrument.setExchangeId(item.getExchangeId());
                instrument.setInstrumentId(item.getInstrumentId());
                instrument.setMultiple(item.getMultiple());
                instrument.setUpdateTime(item.getUpdateTime());
                instrument.setVolumeCommission(item.getVolumeCommission());
                instrument.setVolumeMargin(item.getVolumeMargin());
                return instrument;
            }).forEachOrdered(instrument -> {
                instruments.add(instrument);
            });
            return instruments;
        }
    }

    @Override
    public Set<Order> selectOrders() throws DataQueryException {
        synchronized (orderTable) {
            var orders = new HashSet<Order>();
            orderTable.values().stream().map(item -> {
                var order = ObjectFactory.newOrder();
                order.setDirection(item.getDirection());
                order.setInstrumentId(item.getInstrumentId());
                order.setOffset(item.getOffset());
                order.setOrderId(item.getOrderId());
                order.setPrice(item.getPrice());
                order.setTradingDay(item.getTradingDay());
                order.setTransactionId(item.getTransactionId());
                order.setVolume(item.getVolume());
                return order;
            }).forEachOrdered(order -> {
                orders.add(order);
            });
            return orders;
        }
    }

    @Override
    public Set<StrategyProfile> selectStrategyProfiles() throws DataQueryException {
        synchronized (profileTable) {
            var profiles = new HashSet<StrategyProfile>();
            profileTable.values().stream().map(item -> {
                var profile = ObjectFactory.newStrategyProfile();
                profile.setArgs(item.getArgs());
                profile.setCreateDate(item.getCreateDate());
                profile.setInstrumentIds(item.getInstrumentIds());
                profile.setPassword(item.getPassword());
                profile.setRemoveDate(item.getRemoveDate());
                profile.setState(item.getState());
                profile.setStrategyId(item.getStrategyId());
                profile.setUserId(item.getUserId());
                return profile;
            }).forEachOrdered(profile -> {
                profiles.add(profile);
            });
            return profiles;
        }
    }

    @Override
    public Set<Trade> selectTrades() throws DataQueryException {
        synchronized (tradeTable) {
            var trades = new HashSet<Trade>();
            trades.addAll(tradeTable.values());
            return trades;
        }
    }

    @Override
    public Set<Transaction> selectTransactions() throws DataQueryException {
        synchronized (transactionTable) {
            var transactions = new HashSet<Transaction>();
            transactionTable.values().stream().map(item -> {
                var transaction = ObjectFactory.newTransaction();
                transaction.setDirection(item.getDirection());
                transaction.setInstrumentId(item.getInstrumentId());
                transaction.setOffset(item.getOffset());
                transaction.setPrice(item.getPrice());
                transaction.setState(item.getState());
                transaction.setStateMessage(item.getStateMessage());
                transaction.setStrategyId(item.getStrategyId());
                transaction.setTradingDay(item.getTradingDay());
                transaction.setTransactionId(item.getTransactionId());
                transaction.setUpdateTime(item.getUpdateTime());
                transaction.setVolume(item.getVolume());
                return transaction;
            }).forEachOrdered(transaction -> {
                transactions.add(transaction);
            });
            return transactions;
        }
    }

    @Override
    public Set<User> selectUsers() throws DataQueryException {
        synchronized (userTable) {
            var users = new HashSet<User>();
            userTable.values().stream().map(item -> {
                var user = ObjectFactory.newUser();
                user.setCreateTime(item.getCreateTime());
                user.setLastLoginTime(item.getLastLoginTime());
                user.setPassword(item.getPassword());
                user.setUserId(item.getUserId());
                return user;
            }).forEachOrdered(user -> {
                users.add(user);
            });
            return users;
        }
    }

    @Override
    public Set<Tick> selectTicks() throws DataQueryException {
        synchronized (tickTable) {
            var ticks = new HashSet<Tick>();
            ticks.addAll(tickTable.values());
            return ticks;
        }
    }

    private <T> Table<T> readTable(Class<T> clazz) {
        var target = tablePath(clazz.getCanonicalName());
        try (FileReader fileReader = new FileReader(target.toFile())) {
            return g.fromJson(fileReader, new Table<T>().getClass());
        } catch (IOException exception) {
            Utils.err.write("Can't read table: " + exception.getMessage(), exception);
            var table = new Table<T>();
            table.name = clazz.getCanonicalName();
            return table;
        }
    }

    private Path tablePath(String name) {
        return Paths.get(Utils.cwd().toString(), "Schema", name);
    }

    private void writeTradingDay() {
        var table = new Table<TradingDay>();
        table.name = TradingDay.class.getCanonicalName();
        table.updateTime = Utils.datetime();
        table.rows.add(tradingDay.get());
        writeJsonTable(table);
    }

    private void writeJsonTable(Table table) {
        var target = tablePath(table.name);
        Utils.file(target);
        writeJson(target.toFile(), table);
    }

    private <T> void writeTable(Class<T> clazz, Collection<T> rows) {
        var table = new Table<T>();
        table.name = clazz.getCanonicalName();
        table.updateTime = Utils.datetime();
        table.rows.addAll(rows);
        writeJsonTable(table);
    }

    private void writeJson(File target, Object object) {
        try (FileWriter fileWriter = new FileWriter(target, false)) {
            /* write json */
            fileWriter.write(g.toJson(object));
        } catch (IOException exception) {
            Utils.err.write("Can't write schema: " + exception.getMessage(), exception);
        }
    }

    private void ensureNotExists(Map<?, ?> map, Object key) throws DataQueryException {
        if (map.containsKey(key)) {
            throw new DataQueryException("Duplicated key: " + key + ".");
        }
    }

    private void ensureExists(Map<?, ?> map, Object key) throws DataQueryException {
        if (!map.containsKey(key)) {
            throw new DataQueryException("Key(" + key + ") doesn't exist.");
        }
    }

    private class Table<T> {

        String name;
        String updateTime;
        Set<T> rows = new HashSet<>();
    }

    private class QuerySchema {

        String backupTime;
        TradingDay tradingDay;
        Set<Account> accounts = new HashSet<>();
        Set<Tick> ticks = new HashSet<>();
        Set<Transaction> transactions = new HashSet<>();
        Set<Order> orders = new HashSet<>();
        Set<Trade> trades = new HashSet<>();
        Set<Contract> contracts = new HashSet<>();
        Set<User> users = new HashSet<>();
        Set<StrategyProfile> strategyProfiles = new HashSet<>();
        Set<Instrument> instruments = new HashSet<>();
    }

}
