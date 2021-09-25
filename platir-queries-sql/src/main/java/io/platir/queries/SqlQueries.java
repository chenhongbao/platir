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
import java.util.HashSet;
import java.util.Set;

/**
 *
 * @author Chen Hongbao
 */
public abstract class SqlQueries implements Queries {

    @Override
    public void prepareTables() throws SQLException {
        final String TRADING_DAY_CREATE_SQL = "CREATE TABLE TRADING_DAY_T ("
                + "TRADING_DAY CHAR(64), UPDATE_TIME CHAR(64))";
        final String ACCOUNT_CREATE_SQL = "CREATE TABLE ACCOUNT_T (ACCOUNT_ID CHAR(64),"
                + "USER_ID CHAR(64), BALANCE DOUBLE, MARGIN DOUBLE, COMMISSION DOUBLE,"
                + "OPENING_MARGIN DOUBLE, OPENING_COMMISSION DOUBLE, CLOSING_COMMISSION DOUBLE,"
                + "AVAILABLE DOUBLE, POSITION_PROFIT DOUBLE, CLOSE_PROFIT DOUBLE,"
                + "YD_BALANCE DOUBLE, TRADING_DAY CHAR(64), SETTLE_TIME CHAR(64))";
        tryCreateTable("TRADING_DAY_T", TRADING_DAY_CREATE_SQL);
        tryCreateTable("ACCOUNT_T", ACCOUNT_CREATE_SQL);
    }

    @Override
    public void insert(TradingDay day) throws SQLException {
        final String TRADING_DAY_INSERT_SQL = "INSERT INTO TRADING_DAY_T VALUES (?, ?')";
        tryInsertTable(TRADING_DAY_INSERT_SQL, day.getTradingDay(), day.getUpdateTime());
    }

    @Override
    public void insert(Account... accounts) throws SQLException {
        if (accounts.length > 0) {
            final String ACCOUNT_INSERT_SQL = "INSERT INTO ACCOUNT_T VALUES "
                    + "(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

            var conn = getConnection();
            var prevCommit = conn.getAutoCommit();
            conn.setAutoCommit(false);
            try (PreparedStatement ps = conn.prepareStatement(ACCOUNT_INSERT_SQL)) {
                for (var a : accounts) {
                    prepareStatement(ps, a.getAccountId(), a.getUserId(),
                            a.getBalance(), a.getMargin(), a.getCommission(),
                            a.getOpeningMargin(), a.getOpeningCommission(),
                            a.getClosingCommission(), a.getAvailable(), a.getPositionProfit(),
                            a.getCloseProfit(), a.getYdBalance(), a.getTradingDay(),
                            a.getSettleTime());
                }
                ps.executeUpdate();
                conn.commit();
            } catch (SQLException ex) {
                conn.rollback();
                throw new SQLException("Batch account insertion failure: " + ex.getMessage(), ex);
            } finally {
                conn.setAutoCommit(prevCommit);
            }
        }
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
        if (accounts.length > 0) {
            final String ACCOUNT_UPDATE_SQL = "UPDATE ACCOUNT_T SET "
                    + "BALANCE=?, MARGIN=?, COMMISSION=?, OPENING_MARGIN=?, OPENING_COMMISSION=?, "
                    + "CLOSING_COMMISSION=?, AVAILABLE=?, POSITION_PROFIT=?, CLOSE_PROFIT=?, "
                    + "YD_BALANCE=?, TRADING_DAY=?, SETTLE_TIME=? "
                    + "WHERE ACCOUNT_ID=? AND USER_ID=?";

            var conn = getConnection();
            var prevCommit = conn.getAutoCommit();
            conn.setAutoCommit(false);
            try (PreparedStatement ps = conn.prepareStatement(ACCOUNT_UPDATE_SQL)) {
                for (var a : accounts) {
                    prepareStatement(ps, a.getBalance(), a.getMargin(), a.getCommission(),
                            a.getOpeningMargin(), a.getOpeningCommission(),
                            a.getClosingCommission(), a.getAvailable(), a.getPositionProfit(),
                            a.getCloseProfit(), a.getYdBalance(), a.getTradingDay(),
                            a.getSettleTime(), a.getAccountId(), a.getUserId());
                }
                ps.executeUpdate();
                conn.commit();
            } catch (SQLException ex) {
                conn.rollback();
                throw new SQLException("Batch account update failure: " + ex.getMessage(), ex);
            } finally {
                conn.setAutoCommit(prevCommit);
            }
        }
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
        if (prev == null) {
            insert(day);
        } else {
            final String TRADING_DAY_UPDATE_SQL = "UPDATE TRADING_DAY_T "
                    + "SET TRADING_DAY = ?, UPDATE_TIME = ? "
                    + "WHERE TRADING_DAY = ? AND UPDATE_TIME= ?";
            try (PreparedStatement ps = getConnection().prepareStatement(TRADING_DAY_UPDATE_SQL)) {
                prepareStatement(ps, day.getTradingDay(), day.getUpdateTime(), 
                        prev.getTradingDay(), prev.getUpdateTime());
                ps.executeUpdate();
            }
        }
    }

    @Override
    public void clearAccounts() throws SQLException {
        final String ACCOUNT_CLEAR_SQL = "DELETE FROM ACCOUNT_T";
        QueryFacilities.executeUpdate(getConnection(), ACCOUNT_CLEAR_SQL);
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
        final String TRADING_DAY_SELECT_SQL = "SELECT * FROM TRADING_DAY_T";
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
        final String ACCOUNT_SELECT_SQL = "SELECT * FROM ACCOUNT_T";
        var r = new HashSet<Account>();
        var rs = QueryFacilities.executeQuery(getConnection(), ACCOUNT_SELECT_SQL);
        while (rs.next()) {
            var a = new Account();
            a.setAccountId(rs.getString("ACCOUNT_ID"));
            a.setUserId(rs.getString("USER_ID"));
            a.setBalance(rs.getDouble("BALANCE"));
            a.setMargin(rs.getDouble("MARGIN"));
            a.setCommission(rs.getDouble("COMMISSION"));
            a.setOpeningMargin(rs.getDouble("OPENING_MARGIN"));
            a.setOpeningCommission(rs.getDouble("OPENING_COMMISSION"));
            a.setClosingCommission(rs.getDouble("CLOSING_COMMISSION"));
            a.setAvailable(rs.getDouble("AVAILABLE"));
            a.setPositionProfit(rs.getDouble("POSITION_PROFIT"));
            a.setCloseProfit(rs.getDouble("CLOSE_PROFIT"));
            a.setYdBalance(rs.getDouble("YD_BALANCE"));
            a.setTradingDay(rs.getString("TRADING_DAY"));
            a.setSettleTime(rs.getString("SETTLE_TIME"));
            r.add(a);
        }
        return r;
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
            prepareStatement(ps, values);
            ps.executeUpdate();
        }
    }

    private void prepareStatement(PreparedStatement ps, Object... values) throws SQLException {
        if (ps == null) {
            throw new SQLException("Prepared statement is null.");
        }
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
    }
}
