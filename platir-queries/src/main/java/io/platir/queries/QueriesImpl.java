package io.platir.queries;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
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
import io.platir.service.Factory;
import io.platir.service.Queries;
import io.platir.service.Schema;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 *
 * @author Chen Hongbao
 */
public class QueriesImpl implements Queries {

    private final Factory factory = new FactoryImpl();
    private final TradingDay tradingDay = new TradingDayImpl();
    private final Map<String, Account> accountTable = new HashMap<>();
    private final Map<String, Tick> tickTable = new HashMap<>();
    private final Map<String, Transaction> transactionTable = new HashMap<>();
    private final Map<String, Order> orderTable = new HashMap<>();
    private final Map<String, Trade> tradeTable = new HashMap<>();
    private final Map<String, Contract> contractTable = new HashMap<>();
    private final Map<String, User> userTable = new HashMap<>();
    private final Map<String, StrategyProfile> profileTable = new HashMap<>();
    private final Map<String, Instrument> instrumentTable = new HashMap<>();
    private final Set<RiskNotice> riskNoticeTable = new HashSet<>();

    private final Gson g;

    public QueriesImpl() {
        g = createGson();
    }

    @Override
    public void initialize() throws DataQueryException {
        accountTable.clear();
        readTable(AccountImpl.class, Account.class.getSimpleName()).rows().forEach(item -> {
            accountTable.put(item.getAccountId(), item);
        });
        tickTable.clear();
        readTable(TickImpl.class, Tick.class.getSimpleName()).rows().forEach(item -> {
            tickTable.put(item.getInstrumentId(), item);
        });
        transactionTable.clear();
        readTable(TransactionImpl.class, Transaction.class.getSimpleName()).rows().forEach(item -> {
            transactionTable.put(item.getTransactionId(), item);
        });
        orderTable.clear();
        readTable(OrderImpl.class, Order.class.getSimpleName()).rows().forEach(item -> {
            orderTable.put(item.getOrderId(), item);
        });
        tradeTable.clear();
        readTable(TradeImpl.class, Trade.class.getSimpleName()).rows().forEach(item -> {
            tradeTable.put(item.getTradeId(), item);
        });
        contractTable.clear();
        readTable(ContractImpl.class, Contract.class.getSimpleName()).rows().forEach(item -> {
            contractTable.put(item.getContractId(), item);
        });
        userTable.clear();
        readTable(UserImpl.class, User.class.getSimpleName()).rows().forEach(item -> {
            userTable.put(item.getUserId(), item);
        });
        profileTable.clear();
        readTable(StrategyProfileImpl.class, StrategyProfile.class.getSimpleName()).rows().forEach(item -> {
            profileTable.put(item.getStrategyId(), item);
        });
        instrumentTable.clear();
        readTable(InstrumentImpl.class, Instrument.class.getSimpleName()).rows().forEach(item -> {
            instrumentTable.put(item.getInstrumentId(), item);
        });
        var days = readTable(TradingDayImpl.class, TradingDay.class.getSimpleName()).rows();
        if (days.size() > 1) {
            throw new DataQueryException("Duplicated TradingDay.");
        } else if (days.size() == 1) {
            var day = days.iterator().next();
            tradingDay.setDay(day.getDay());
            tradingDay.setUpdateTime(day.getUpdateTime());
        }
    }

    @Override
    public Schema backup(Path directory) {
        try {
            Utils.dir(directory);
            writeTradingDay(filePath(directory, TradingDay.class.getSimpleName()));
            writeTable(filePath(directory, Account.class.getSimpleName()), Account.class, accountTable.values());
            writeTable(filePath(directory, Tick.class.getSimpleName()), Tick.class, tickTable.values());
            writeTable(filePath(directory, Transaction.class.getSimpleName()), Transaction.class, transactionTable.values());
            writeTable(filePath(directory, Order.class.getSimpleName()), Order.class, orderTable.values());
            writeTable(filePath(directory, Trade.class.getSimpleName()), Trade.class, tradeTable.values());
            writeTable(filePath(directory, Contract.class.getSimpleName()), Contract.class, contractTable.values());
            writeTable(filePath(directory, User.class.getSimpleName()), User.class, userTable.values());
            writeTable(filePath(directory, StrategyProfile.class.getSimpleName()), StrategyProfile.class, profileTable.values());
            writeTable(filePath(directory, Instrument.class.getSimpleName()), Instrument.class, instrumentTable.values());
            return buildSchema();
        } catch (DataQueryException exception) {
            Utils.err().write("Can't backup schema: " + exception.getMessage(), exception);
            return null;
        }
    }

    @Override
    public Schema restore(Path backupDirectory) throws DataQueryException {
        try {
            clearRuntime();
            copySchema(backupDirectory);
            initialize();
            return buildSchema();
        } catch (IOException ex) {
            throw new DataQueryException("Can't copy backup files to schema.");
        }
    }

    @Override
    public void insert(TradingDay day) throws DataQueryException {
        if (day != null) {
            tradingDay.setDay(day.getDay());
            tradingDay.setUpdateTime(day.getUpdateTime());
            /* persist */
            writeTradingDay(schemaTablePath(TradingDay.class.getSimpleName()));
        }
    }

    @Override
    public void insert(Account... accounts) throws DataQueryException {
        synchronized (accountTable) {
            for (var account : accounts) {
                if (account.getAccountId() == null) {
                    throw new DataQueryException("Account has a null ID.");
                }
                ensureNotExists(accountTable, account.getAccountId());
                accountTable.put(account.getAccountId(), copyOf(account));
            }
            /* persist account table */
            writeTable(schemaTablePath(Account.class.getSimpleName()), Account.class, accountTable.values());
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
                tickTable.put(tick.getInstrumentId(), copyOf(tick));
            }
            writeTable(schemaTablePath(Tick.class.getSimpleName()), Tick.class, tickTable.values());
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
                transactionTable.put(transaction.getTransactionId(), copyOf(transaction));
            }
            writeTable(schemaTablePath(Transaction.class.getSimpleName()), Transaction.class, transactionTable.values());
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
                orderTable.put(order.getOrderId(), copyOf(order));
            }
            writeTable(schemaTablePath(Order.class.getSimpleName()), Order.class, orderTable.values());
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
                tradeTable.put(trade.getTradeId(), copyOf(trade));
            }
            writeTable(schemaTablePath(Trade.class.getSimpleName()), Trade.class, tradeTable.values());
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
                contractTable.put(contract.getContractId(), copyOf(contract));
            }
            writeTable(schemaTablePath(Contract.class.getSimpleName()), Contract.class, contractTable.values());
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
                userTable.put(user.getUserId(), copyOf(user));
            }
            writeTable(schemaTablePath(User.class.getSimpleName()), User.class, userTable.values());
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
                profileTable.put(profile.getStrategyId(), copyOf(profile));
            }
            writeTable(schemaTablePath(StrategyProfile.class.getSimpleName()), StrategyProfile.class, profileTable.values());
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
                instrumentTable.put(instrument.getInstrumentId(), copyOf(instrument));
            }
            writeTable(schemaTablePath(Instrument.class.getSimpleName()), Instrument.class, instrumentTable.values());
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
                accountTable.put(account.getAccountId(), copyOf(account));
            }
            writeTable(schemaTablePath(Account.class.getSimpleName()), Account.class, accountTable.values());
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
                contractTable.put(contract.getContractId(), copyOf(contract));
            }
            writeTable(schemaTablePath(Contract.class.getSimpleName()), Contract.class, contractTable.values());
        }
    }

    @Override
    public void update(Order... orders) throws DataQueryException {
        synchronized (orderTable) {
            for (var order : orders) {
                if (order.getOrderId() == null) {
                    throw new DataQueryException("Order has a null ID.");
                }
                ensureExists(orderTable, order.getOrderId());
                orderTable.put(order.getOrderId(), copyOf(order));
            }
            writeTable(schemaTablePath(Order.class.getSimpleName()), Order.class, orderTable.values());
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
                transactionTable.put(transaction.getTransactionId(), copyOf(transaction));
            }
            writeTable(schemaTablePath(Transaction.class.getSimpleName()), Transaction.class, transactionTable.values());
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
                instrumentTable.put(instrument.getInstrumentId(), copyOf(instrument));
            }
            writeTable(schemaTablePath(Instrument.class.getSimpleName()), Instrument.class, instrumentTable.values());
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
                userTable.put(user.getUserId(), copyOf(user));
            }
            writeTable(schemaTablePath(User.class.getSimpleName()), User.class, userTable.values());
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
                profileTable.put(profile.getStrategyId(), copyOf(profile));
            }
            writeTable(schemaTablePath(StrategyProfile.class.getSimpleName()), StrategyProfile.class, profileTable.values());
        }
    }

    @Override
    public void update(TradingDay... days) throws DataQueryException {
        if (days == null || days.length == 0) {
            return;
        }
        var list = Arrays.asList(days);
        list.sort((TradingDay day1, TradingDay day2) -> day2.getUpdateTime().compareTo(day1.getUpdateTime()));
        var day = list.get(0);
        tradingDay.setDay(day.getDay());
        tradingDay.setUpdateTime(day.getUpdateTime());
        writeTradingDay(schemaTablePath(TradingDay.class.getSimpleName()));
    }

    @Override
    public void clearAccounts() throws DataQueryException {
        synchronized (accountTable) {
            accountTable.clear();
            writeTable(schemaTablePath(Account.class.getSimpleName()), Account.class, accountTable.values());
        }
    }

    @Override
    public void clearContracts() throws DataQueryException {
        synchronized (contractTable) {
            contractTable.clear();
            writeTable(schemaTablePath(Contract.class.getSimpleName()), Contract.class, contractTable.values());
        }
    }

    @Override
    public void clearOrders() throws DataQueryException {
        synchronized (orderTable) {
            orderTable.clear();
            writeTable(schemaTablePath(Order.class.getSimpleName()), Order.class, orderTable.values());
        }
    }

    @Override
    public void clearTrades() throws DataQueryException {
        synchronized (tradeTable) {
            tradeTable.clear();
            writeTable(schemaTablePath(Trade.class.getSimpleName()), Trade.class, tradeTable.values());
        }
    }

    @Override
    public void clearTransactions() throws DataQueryException {
        synchronized (transactionTable) {
            transactionTable.clear();
            writeTable(schemaTablePath(Transaction.class.getSimpleName()), Transaction.class, transactionTable.values());
        }
    }

    @Override
    public void clearTicks() throws DataQueryException {
        synchronized (tickTable) {
            tickTable.clear();
            writeTable(schemaTablePath(Tick.class.getSimpleName()), Tick.class, tickTable.values());
        }
    }

    @Override
    public void clearStrategies() throws DataQueryException {
        synchronized (profileTable) {
            profileTable.clear();
            writeTable(schemaTablePath(StrategyProfile.class.getSimpleName()), StrategyProfile.class, profileTable.values());
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
        var day = factory.newTradingDay();
        day.setDay(tradingDay.getDay());
        day.setUpdateTime(tradingDay.getUpdateTime());
        return day;
    }

    @Override
    public Set<Account> selectAccounts() throws DataQueryException {
        synchronized (accountTable) {
            var accounts = new HashSet<Account>();
            accountTable.values().stream().map(item -> {
                return copyOf(item);
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
                return copyOf(item);
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
                return copyOf(item);
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
                return copyOf(item);
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
                return copyOf(item);
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
            tradeTable.values().stream().map(item -> {
                return copyOf(item);
            }).forEachOrdered(trade -> {
                trades.add(trade);
            });
            return trades;
        }
    }

    @Override
    public Set<Transaction> selectTransactions() throws DataQueryException {
        synchronized (transactionTable) {
            var transactions = new HashSet<Transaction>();
            transactionTable.values().stream().map(item -> {
                return copyOf(item);
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
                return copyOf(item);
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
            tickTable.values().stream().map(item -> {
                return copyOf(item);
            }).forEach(tick -> {
                ticks.add(tick);
            });
            return ticks;
        }
    }

    @Override
    public Set<RiskNotice> selectRiskNotices() throws DataQueryException {
        synchronized (riskNoticeTable) {
            var notices = new HashSet<RiskNotice>();
            riskNoticeTable.stream().map(item -> {
                return copyOf(item);
            }).forEachOrdered(notice -> {
                notices.add(notice);
            });
            return notices;
        }
    }

    @Override
    public Factory getFactory() {
        return factory;
    }

    @Override
    public void remove(Contract... contracts) throws DataQueryException {
        if (contracts == null || contracts.length == 0) {
            return;
        }
        synchronized (contractTable) {
            for (var contract : contracts) {
                if (contractTable.containsKey(contract.getContractId())) {
                    contractTable.remove(contract.getContractId());
                }
            }
            writeTable(schemaTablePath(Contract.class.getSimpleName()), Contract.class, contractTable.values());
        }
    }

    private Account copyOf(Account item) {
        var account = factory.newAccount();
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
    }

    private Contract copyOf(Contract item) {
        var contract = factory.newContract();
        contract.setClosePrice(item.getClosePrice());
        contract.setContractId(item.getContractId());
        contract.setDirection(item.getDirection());
        contract.setInstrumentId(item.getInstrumentId());
        contract.setOpenTime(item.getOpenTime());
        contract.setOpenTradingDay(item.getOpenTradingDay());
        contract.setPrice(item.getPrice());
        contract.setState(item.getState());
        contract.setUserId(item.getUserId());
        contract.setSettlementTradingDay(item.getSettlementTradingDay());
        return contract;
    }

    private Instrument copyOf(Instrument item) {
        var instrument = factory.newInstrument();
        instrument.setAmountCommission(item.getAmountCommission());
        instrument.setAmountMargin(item.getAmountMargin());
        instrument.setExchangeId(item.getExchangeId());
        instrument.setInstrumentId(item.getInstrumentId());
        instrument.setMultiple(item.getMultiple());
        instrument.setUpdateTime(item.getUpdateTime());
        instrument.setVolumeCommission(item.getVolumeCommission());
        instrument.setVolumeMargin(item.getVolumeMargin());
        return instrument;
    }

    private Order copyOf(Order item) {
        var order = factory.newOrder();
        order.setDirection(item.getDirection());
        order.setInstrumentId(item.getInstrumentId());
        order.setOffset(item.getOffset());
        order.setOrderId(item.getOrderId());
        order.setPrice(item.getPrice());
        order.setTradingDay(item.getTradingDay());
        order.setTransactionId(item.getTransactionId());
        order.setVolume(item.getVolume());
        order.setState(item.getState());
        return order;
    }

    private StrategyProfile copyOf(StrategyProfile item) {
        var profile = factory.newStrategyProfile();
        profile.setArgs(item.getArgs());
        profile.setCreateDate(item.getCreateDate());
        profile.setInstrumentIds(item.getInstrumentIds());
        profile.setPassword(item.getPassword());
        profile.setRemoveDate(item.getRemoveDate());
        profile.setState(item.getState());
        profile.setStrategyId(item.getStrategyId());
        profile.setUserId(item.getUserId());
        profile.setStrategyProfileId(item.getStrategyProfileId());
        return profile;
    }

    private Trade copyOf(Trade item) {
        var trade = factory.newTrade();
        trade.setDirection(item.getDirection());
        trade.setInstrumentId(item.getInstrumentId());
        trade.setOffset(item.getOffset());
        trade.setOrderId(item.getOrderId());
        trade.setPrice(item.getPrice());
        trade.setTradeId(item.getTradeId());
        trade.setTradingDay(item.getTradingDay());
        trade.setUpdateTime(item.getUpdateTime());
        trade.setVolume(item.getVolume());
        return trade;
    }

    private Transaction copyOf(Transaction item) {
        var transaction = factory.newTransaction();
        transaction.setDirection(item.getDirection());
        transaction.setInstrumentId(item.getInstrumentId());
        transaction.setOffset(item.getOffset());
        transaction.setPrice(item.getPrice());
        transaction.setState(item.getState());
        transaction.setStrategyId(item.getStrategyId());
        transaction.setTradingDay(item.getTradingDay());
        transaction.setTransactionId(item.getTransactionId());
        transaction.setUpdateTime(item.getUpdateTime());
        transaction.setVolume(item.getVolume());
        return transaction;
    }

    private User copyOf(User item) {
        var user = factory.newUser();
        user.setCreateTime(item.getCreateTime());
        user.setLastLoginTime(item.getLastLoginTime());
        user.setPassword(item.getPassword());
        user.setUserId(item.getUserId());
        return user;
    }

    private Tick copyOf(Tick item) {
        var tick = factory.newTick();
        tick.setAskPrice(item.getAskPrice());
        tick.setAskVolume(item.getAskVolume());
        tick.setBidPrice(item.getBidPrice());
        tick.setBidVolume(item.getBidVolume());
        tick.setClosePrice(item.getClosePrice());
        tick.setInstrumentId(item.getInstrumentId());
        tick.setLastPrice(item.getLastPrice());
        tick.setOpenInterest(item.getOpenInterest());
        tick.setOpenPrice(item.getOpenPrice());
        tick.setSettlementPrice(item.getSettlementPrice());
        tick.setTickId(item.getTickId());
        tick.setTodayVolume(item.getTodayVolume());
        tick.setUpdateTime(item.getUpdateTime());
        tick.setTradingDay(item.getTradingDay());
        tick.setHighPrice(item.getHighPrice());
        tick.setLowPrice(item.getLowPrice());
        return tick;
    }

    private RiskNotice copyOf(RiskNotice item) {
        var notice = factory.newRiskNotice();
        notice.setCode(item.getCode());
        notice.setLevel(notice.getLevel());
        notice.setMessage(notice.getMessage());
        notice.setStrategyId(notice.getStrategyId());
        notice.setUpdateTime(notice.getUpdateTime());
        notice.setUserId(notice.getUserId());
        notice.setRiskNoticeId(item.getRiskNoticeId());
        return notice;
    }

    private void clearRuntime() {
        tradingDay.setDay(null);
        tradingDay.setUpdateTime(null);
        instrumentTable.clear();
        userTable.clear();
        accountTable.clear();
        contractTable.clear();
        orderTable.clear();
        profileTable.clear();
        tickTable.clear();
        tradeTable.clear();
        transactionTable.clear();
    }

    private QuerySchema buildSchema() throws DataQueryException {
        var schema = new QuerySchema();
        schema.setLastModifiedTime(Utils.datetime());
        schema.setTradingDay(tradingDay);
        schema.setAccounts(selectAccounts());
        schema.setTicks(selectTicks());
        schema.setTransactions(selectTransactions());
        schema.setOrders(selectOrders());
        schema.setTrades(selectTrades());
        schema.setContracts(selectContracts());
        schema.setUsers(selectUsers());
        schema.setStrategyProfiles(selectStrategyProfiles());
        schema.setInstruments(selectInstruments());
        return schema;
    }

    private <T> Table<T> readTable(Class<T> clazz, String name) throws DataQueryException {
        var table = new Table<T>();
        table.setName(name);
        var target = schemaTablePath(name);
        if (!Files.exists(target) || target.toFile().length() == 0) {
            return table;
        }
        try (FileReader fileReader = new FileReader(target.toFile())) {
            return g.fromJson(fileReader, getProperTypeToken(clazz).getType());
        } catch (Throwable throwable) {
            Utils.err().write("Can't read table: " + throwable.getMessage(), throwable);
            return table;
        }
    }

    private TypeToken<?> getProperTypeToken(Class<?> clazz) throws DataQueryException {
        if (clazz == AccountImpl.class) {
            return TypeToken.getParameterized(Table.class, AccountImpl.class);
        } else if (clazz == TickImpl.class) {
            return TypeToken.getParameterized(Table.class, TickImpl.class);
        } else if (clazz == TransactionImpl.class) {
            return TypeToken.getParameterized(Table.class, TransactionImpl.class);
        } else if (clazz == OrderImpl.class) {
            return TypeToken.getParameterized(Table.class, OrderImpl.class);
        } else if (clazz == TradeImpl.class) {
            return TypeToken.getParameterized(Table.class, TradeImpl.class);
        } else if (clazz == ContractImpl.class) {
            return TypeToken.getParameterized(Table.class, ContractImpl.class);
        } else if (clazz == UserImpl.class) {
            return TypeToken.getParameterized(Table.class, UserImpl.class);
        } else if (clazz == StrategyProfileImpl.class) {
            return TypeToken.getParameterized(Table.class, StrategyProfileImpl.class);
        } else if (clazz == InstrumentImpl.class) {
            return TypeToken.getParameterized(Table.class, InstrumentImpl.class);
        } else if (clazz == TradingDayImpl.class) {
            return TypeToken.getParameterized(Table.class, TradingDayImpl.class);
        } else {
            throw new DataQueryException("Unsupported table type: " + clazz.getCanonicalName() + ".");
        }
    }

    private Path schemaTablePath(String name) {
        return filePath(Utils.schemaDirectory(), name);
    }

    private Path filePath(Path directory, String fileName) {
        return Paths.get(directory.toString(), fileName);
    }

    private void writeTradingDay(Path target) {
        var table = new Table<TradingDay>();
        table.setName(TradingDay.class.getCanonicalName());
        table.setUpdateTime(Utils.datetime());
        table.rows().add(tradingDay);
        writeJsonTable(target, table);
    }

    private void writeJsonTable(Path target, Table table) {
        Utils.file(target);
        writeJson(target.toFile(), table);
    }

    private <T> void writeTable(Path target, Class<T> clazz, Collection<T> rows) {
        var table = new Table<T>();
        table.setName(clazz.getCanonicalName());
        table.setUpdateTime(Utils.datetime());
        table.rows().addAll(rows);
        writeJsonTable(target, table);
    }

    private void writeJson(File target, Object object) {
        try (FileWriter fileWriter = new FileWriter(target, false)) {
            /* write json */
            fileWriter.write(g.toJson(object));
        } catch (IOException exception) {
            Utils.err().write("Can't write schema: " + exception.getMessage(), exception);
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

    private Gson createGson() {
        var builder = new Gson().newBuilder().serializeNulls().setPrettyPrinting();
        return builder.create();
    }

    private void copySchema(Path backupDirectory) throws IOException {
        Utils.delete(Utils.schemaDirectory());
        Utils.copyEntries(backupDirectory, Utils.schemaDirectory());
    }

}
