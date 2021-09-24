package io.platir.queries;

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
import io.platir.service.api.Queries;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Set;

/**
 *
 * @author Chen Hongbao
 */
public abstract class SqlQueries implements Queries {

    private static final String TRADING_DAY_CREATE_SQL = "CREATE TABLE TRADING_DAY_T ("
            + "TRADING_DAY CHAR(64), "
            + "UPDATE_TIME CHAR(64))";

    private static final String TRADING_DAY_INSERT_SQL = "INSERT INTO TRADING_DAY_T VALUES (?, ?')";

    private static final String TRADING_DAY_UPDATE_SQL = "UPDATE TRADING_DAY_T "
            + "SET TRADING_DAY = ?, UPDATE_TIME = ? "
            + "WHERE TRADING_DAY = ? AND UPDATE_TIME= ?";

    private static final String TRADING_DAY_SELECT_SQL = "SELECT * FROM TRADING_DAY_T";

    private static final String ACCOUNT_CREATE_SQL = "CREATE TABLE ACCOUNT_T ("
            + "ACCOUNT_ID CHAR(64),"
            + "USER_ID CHAR(64),"
            + "BALANCE DOUBLE,"
            + "MARGIN DOUBLE,"
            + "COMMISSION DOUBLE,"
            + "OPENING_MARGIN DOUBLE,"
            + "OPENING_COMMISSION DOUBLE,"
            + "CLOSING_COMMISSION DOUBLE,"
            + "AVAILABLE DOUBLE,"
            + "POSITION_PROFIT DOUBLE,"
            + "CLOSE_PROFIT DOUBLE,"
            + "YD_BALANCE DOUBLE,"
            + "TRADING_DAY CHAR(64),"
            + "SETTLE_TIME CHAR(64))";

    @Override
    public void prepareTables() throws SQLException {
        tryCreateTable("TRADING_DAY_T", TRADING_DAY_CREATE_SQL);
        tryCreateTable("ACCOUNT_T", ACCOUNT_CREATE_SQL);
    }

    @Override
    public void insert(TradingDay day) throws SQLException {
        tryInsertTable(TRADING_DAY_INSERT_SQL, day.getTradingDay(), day.getUpdateTime());
    }

    @Override
    public void insert(Account... accounts) throws SQLException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void insert(Tick... ticks) throws SQLException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void insert(Transaction... transactions) throws SQLException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void insert(Order... orders) throws SQLException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void insert(Trade... trades) throws SQLException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void insert(Contract... contracts) throws SQLException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void insert(User... users) throws SQLException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void insert(StrategyProfile... profiles) throws SQLException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void insert(Instrument... instruments) throws SQLException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void insert(RiskNotice... notices) throws SQLException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void update(Account... accounts) throws SQLException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void update(Contract... contracts) throws SQLException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void update(Transaction... transactions) throws SQLException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void update(Instrument... instruments) throws SQLException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void update(User... users) throws SQLException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void update(StrategyProfile... profiles) throws SQLException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void updateTradingDay(TradingDay day) throws SQLException {
        var prev = selectTradingDay();
        try (PreparedStatement ps = getConnection().prepareStatement(TRADING_DAY_UPDATE_SQL)) {
            ps.setString(1, day.getTradingDay());
            ps.setString(2, day.getUpdateTime());
            ps.setString(3, prev.getTradingDay());
            ps.setString(4, prev.getUpdateTime());
            ps.executeUpdate();
        }
    }

    @Override
    public void clearAccounts() throws SQLException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void clearContracts() throws SQLException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void clearOrders() throws SQLException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void clearTrades() throws SQLException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void clearTransactions() throws SQLException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void clearTicks() throws SQLException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void clearStrategies() throws SQLException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public TradingDay selectTradingDay() throws SQLException {
        var rs = QueryFacilities.executeQuery(getConnection(), TRADING_DAY_SELECT_SQL);
        if (!rs.next()) {
            return null;
        }
        var r = new TradingDay();
        r.setTradingDay(rs.getString("TRADING_DAY"));
        r.setUpdateTime(rs.getString("UPDATE_TIME"));
        return r;
    }

    @Override
    public Set<Account> selectAccounts() throws SQLException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Set<Contract> selectContracts() throws SQLException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Set<Instrument> selectInstruments() throws SQLException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Set<Order> selectOrders() throws SQLException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Set<StrategyProfile> selectStrategyProfiles() throws SQLException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Set<Trade> selectTrades() throws SQLException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Set<Transaction> selectTransactions() throws SQLException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Set<User> selectUsers() throws SQLException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Set<Tick> selectTicks() throws SQLException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    protected abstract Connection getConnection();

    private void tryCreateTable(String table, String sql) throws SQLException {
        if (QueryFacilities.tableExists(getConnection(), table)) {
            return;
        }
        QueryFacilities.executeUpdate(getConnection(), sql);
    }

    private void tryInsertTable(String sql, Object... values) throws SQLException {
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            for (int count = 1; count <= values.length; ++count) {
                var obj = values[count - 1];
                if (obj.getClass() == Integer.class || obj.getClass() == int.class) {
                    ps.setInt(count, (Integer) obj);
                } else if (obj.getClass() == String.class) {
                    ps.setString(count, (String) obj);
                } else if (obj.getClass() == Double.class) {
                    ps.setDouble(count, (Double) obj);
                } else {
                    throw new SQLException("Unsupported value type: " + obj.getClass() + ".");
                }
            }
            ps.executeUpdate();
        }
    }
}
