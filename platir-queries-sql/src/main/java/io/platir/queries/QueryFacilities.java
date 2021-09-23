package io.platir.queries;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Database operation primitives.
 *
 * @author Chen Hongbao
 */
public class QueryFacilities {

    public static boolean tableExists(Connection connection, String tableName) throws SQLException {
        var table = connection.getMetaData().getTables(null, null, tableName, new String[]{"TABLE"});
        return table.next();
    }

    public static int executeUpdate(Connection connection, String sql) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            return statement.executeUpdate(sql);
        }
    }

    public static int[] executeUpdate(Connection connection, String... sqls) throws SQLException {
        boolean prevAuto = true;
        int[] updateCounts;
        try (Statement statement = connection.createStatement()) {
            prevAuto = connection.getAutoCommit();
            connection.setAutoCommit(false);
            for (var sql : sqls) {
                statement.addBatch(sql);
            }
            updateCounts = statement.executeBatch();
            connection.commit();
            return updateCounts;
        } catch (SQLException ex) {
            /* roll back on error */
            connection.rollback();
            throw new SQLException("Batch update failure: " + ex.getMessage(), ex);
        } finally {
            /* restore autocommit */
            connection.setAutoCommit(prevAuto);
        }
    }

    public static ResultSet executeQuery(Connection connection, String sql) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            return statement.executeQuery(sql);
        }
    }
}
