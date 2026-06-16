package com.veeva.vault.toolbox.core.sql;

import com.veeva.vault.vapil.api.model.VaultModel;

import java.io.File;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provides an interface to interact with a SQLite database.
 */
public class Sqlite {

    private static final Logger logger = LoggerFactory.getLogger(Sqlite.class);

    private final Integer BATCH_SIZE = 10000;

    private File dbFile = null;
    private Connection conn = null;
    private DatabaseMetaData dbMetadata = null;

    private int numStatements = 0;
    private StringBuilder sqlCachedStatements = new StringBuilder();
    private Map<String, StringBuilder> tableToStatementBuilder = new HashMap<>();
    private Map<String, AtomicInteger> tableToStatementCount = new HashMap<>();

    /**
     * Constructs a new Sqlite instance for the specified database file.
     *
     * @param dbFile the SQLite database file
     */
    public Sqlite(File dbFile) {
        this.dbFile = dbFile;
        try {
            Class.forName("org.sqlite.JDBC");
            conn = DriverManager.getConnection("jdbc:sqlite:" + dbFile.getAbsolutePath());
            dbMetadata = conn.getMetaData();
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

    /**
     * Creates a table with the specified name and fields.
     *
     * @param tableName  the name of the table
     * @param fieldNames the list of field names
     * @param drop       whether to drop the table if it already exists
     */
    public void createTable(String tableName, List<String> fieldNames, boolean drop) {
        StringBuilder sqlBuilder = new StringBuilder("");
        if (drop) {
            sqlBuilder.append("DROP TABLE IF EXISTS ").append(tableName).append(";\n");
            sqlBuilder.append("CREATE TABLE ").append(tableName).append(" (\n");
        } else {
            sqlBuilder.append("CREATE TABLE IF NOT EXISTS ").append(tableName).append(" (\n");
        }

        int fieldCount = 0;
        for (String fieldName : fieldNames) {
            fieldCount++;
            sqlBuilder.append("\t").append(fieldName).append(" TEXT");
            if (fieldCount < fieldNames.size()) {
                sqlBuilder.append(",");
            }
            sqlBuilder.append("\n");
        }
        sqlBuilder.append(");");

        execute(sqlBuilder.toString());
    }

    /**
     * Initializes the statement builder and count for a table.
     *
     * @param table the name of the table
     */
    private void initializeTableMaps(String table) {
        if (!tableToStatementBuilder.containsKey(table)) {
            tableToStatementBuilder.put(table, new StringBuilder());
            tableToStatementCount.put(table, new AtomicInteger(-1));
        }
    }

    /**
     * Starts an insert statement for the given table.
     *
     * @param tableName the name of the table
     * @param data      the VaultModel containing the field names
     * @return the SQL string representing the start of the insert statement
     */
    public String startInsertStatement(String tableName, VaultModel data) {
        initializeTableMaps(tableName);
        StringBuilder sqlBuilder = new StringBuilder("INSERT INTO ").append(tableName).append(" (");
        sqlBuilder.append(String.join(",", data.getFieldNames()));
        sqlBuilder.append(")\nVALUES ");
        addBuildingStatement(tableName, sqlBuilder.toString());
        tableToStatementCount.get(tableName).set(0);
        return sqlBuilder.toString();
    }

    /**
     * Adds values to an insert statement for the given table.
     *
     * @param table the name of the table
     * @param data  the VaultModel containing the values
     * @return the SQL string representing the added values
     */
    public String addInsertValues(String table, VaultModel data) {
        StringBuilder sqlBuilder = new StringBuilder("(");
        int fieldCount = 0;
        for (String fieldName : data.getFieldNames()) {
            fieldCount++;
            sqlBuilder.append("\"").append(data.get(fieldName)).append("\"");
            if (fieldCount < data.getFieldNames().size()) {
                sqlBuilder.append(",");
            }
        }
        sqlBuilder.append(")\n");

        addBuildingStatement(table, sqlBuilder.toString());
        return sqlBuilder.toString();
    }

    /**
     * Creates an insert statement using a select query.
     *
     * @param tableName the name of the table
     * @param data      the VaultModel containing the field names and values
     * @return the SQL string representing the insert statement
     */
    public String createInsertStatement(String tableName, VaultModel data) {
        StringBuilder sqlBuilder = new StringBuilder("INSERT INTO ").append(tableName).append(" (");
        sqlBuilder.append(String.join(",", data.getFieldNames()));
        sqlBuilder.append(")\nSELECT ");
        int fieldCount = 0;
        for (String fieldName : data.getFieldNames()) {
            fieldCount++;
            sqlBuilder.append("\"").append(data.get(fieldName)).append("\"");
            if (fieldCount < data.getFieldNames().size()) {
                sqlBuilder.append(",");
            }
        }
        sqlBuilder.append(";\n");

        addStatement(sqlBuilder.toString());
        return sqlBuilder.toString();
    }

    /**
     * Adds an SQL string to the building statement for the given table.
     *
     * @param table the name of the table
     * @param sql   the SQL string to add
     */
    public void addBuildingStatement(String table, String sql) {
        AtomicInteger localNumStatements = tableToStatementCount.get(table);
        StringBuilder localBuilder = tableToStatementBuilder.get(table);
        if (localNumStatements.get() > 0) {
            localBuilder.append(",");
        }
        localBuilder.append(sql);
        localNumStatements.incrementAndGet();
    }

    /**
     * Adds an SQL statement to the batch. Executes the batch if the limit is reached.
     *
     * @param sql the SQL statement to add
     */
    public void addStatement(String sql) {
        sqlCachedStatements.append(sql);
        numStatements++;

        if (numStatements == BATCH_SIZE) {
            execute(sqlCachedStatements.toString());
            sqlCachedStatements.setLength(0);
            numStatements = 0;
        }
    }

    /**
     * Flushes and executes the building statements within a transaction.
     */
    public void flushBuilders() {
        try {
            StringBuilder sb = new StringBuilder();
            sb.append("BEGIN TRANSACTION;\n");
            tableToStatementBuilder.values()
                    .stream()
                    .map(builder -> builder.append(";\n").toString())
                    .forEach(statement -> sb.append(statement));
            sb.append("COMMIT;\n");
            
            execute(sb.toString());
            
            tableToStatementBuilder
                    .values()
                    .forEach(builder -> builder.setLength(0));
            tableToStatementCount
                    .values()
                    .forEach(counter -> counter.set(0));
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

    /**
     * Flushes and executes the cached statements.
     */
    public void flush() {
        try {
            if (sqlCachedStatements != null && sqlCachedStatements.length() > 0) {
                execute(sqlCachedStatements.toString());
                sqlCachedStatements.setLength(0);
                numStatements = 0;
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

    /**
     * Executes the given SQL query and returns the ResultSet.
     *
     * @param sql the SQL query to execute
     * @return the ResultSet of the query
     */
    public ResultSet query(String sql) {
        try {
            Statement stmt = conn.createStatement();
            return stmt.executeQuery(sql);
        } catch (SQLException e) {
            logger.error(e.getMessage(), e);
            return null;
        }
    }

    /**
     * Executes the given SQL statement.
     *
     * @param sql the SQL statement to execute
     */
    public void execute(String sql) {
        if (sql != null && !sql.isEmpty()) {
            try {
                Statement stmt = conn.createStatement();
                stmt.executeUpdate(sql);
            } catch (SQLException e) {
                logger.error("Error executing SQL: {}", sql, e);
            }
        }
    }

    /**
     * Returns a list of all table names in the database.
     *
     * @return the list of table names
     */
    public List<String> getTableNames() {
        return getTableNames(null);
    }

    /**
     * Returns a list of table names matching the given search string.
     *
     * @param search the string to search for in table names
     * @return the list of matching table names
     */
    public List<String> getTableNames(String search) {
        try {
            StringBuilder sql = new StringBuilder();
            sql.append("SELECT name FROM sqlite_master s where s.type='table'");
            if (search != null && !search.isEmpty()) {
                sql.append(" AND name LIKE '%").append(search).append("%'");
            }

            List<String> tableNames = new ArrayList<>();
            Statement stmt = conn.createStatement();
            ResultSet resultSet = stmt.executeQuery(sql.toString());
            if (resultSet != null) {
                while (resultSet.next()) {
                    String tableName = resultSet.getString("name");
                    tableNames.add(tableName);
                }
            }
            return tableNames;
        } catch (SQLException e) {
            logger.error(e.getMessage(), e);
            return null;
        }
    }

    /**
     * Converts a ResultSet row to a VaultModel.
     *
     * @param resultSet the ResultSet
     * @return the VaultModel representation
     */
    public VaultModel convertToModel(ResultSet resultSet) {
        try {
            VaultModel model = new VaultModel();
            ResultSetMetaData metaData = resultSet.getMetaData();
            for (int i = 1; i <= metaData.getColumnCount(); i++) {
                String fieldName = metaData.getColumnName(i);
                model.set(fieldName, resultSet.getString(fieldName));
            }
            return model;
        } catch (SQLException e) {
            logger.error(e.getMessage(), e);
            return null;
        }
    }

    /**
     * Merges multiple tables into a single target table using UNION.
     *
     * @param sourceTableNames the list of source table names
     * @param targetTable      the name of the target table
     */
    public void mergeTables(List<String> sourceTableNames, String targetTable) {
        try {
            StringBuilder sqlBuilder = new StringBuilder();
            sqlBuilder.append("DROP TABLE IF EXISTS ").append(targetTable).append(";\n");
            sqlBuilder.append("CREATE TABLE ").append(targetTable).append(" AS\n");
            int tableCount = 0;
            for (String sourceTableName : sourceTableNames) {
                tableCount++;
                if (tableCount > 1) {
                    sqlBuilder.append("UNION\n");
                }
                sqlBuilder.append("SELECT * FROM ").append(sourceTableName).append("\n");
            }

            execute(sqlBuilder.toString());
        } catch (Exception e) {
            logger.error("Error merging tables", e);
        }
    }
}