package com.bancada.config;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.autoconfigure.orm.jpa.EntityManagerFactoryDependsOnPostProcessor;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

/**
 * Removes the {@code check (column in ('A','B'))} constraints Hibernate writes for enum columns.
 * With {@code ddl-auto: update} they are created once and never refreshed, so a value added to an
 * enum later (a new operation type, a new status) is rejected by every database created before it.
 * The enums are already validated by the application; the check adds nothing but that trap.
 *
 * <p>SQLite cannot drop a constraint with ALTER TABLE, so each affected table is rebuilt (the procedure
 * in the SQLite ALTER TABLE documentation), inside one transaction. Runs before Hibernate starts,
 * so a table Hibernate creates now keeps its checks until the next start, which is harmless: they
 * list every value of the current enums.
 */
@Component
public class EnumCheckCleaner implements InitializingBean {

    private static final Logger LOG = LoggerFactory.getLogger(EnumCheckCleaner.class);
    private static final Pattern ENUM_CHECK =
        Pattern.compile("\\s*check\\s*\\(\\s*\\w+\\s+in\\s*\\(\\s*'[^']*'(?:\\s*,\\s*'[^']*')*\\s*\\)\\s*\\)", Pattern.CASE_INSENSITIVE);
    private static final Pattern CREATE_TABLE_NAME =
        Pattern.compile("^CREATE\\s+TABLE\\s+(\"[^\"]+\"|`[^`]+`|\\[[^]]+]|\\S+)", Pattern.CASE_INSENSITIVE);

    private final DataSource dataSource;

    public EnumCheckCleaner(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void afterPropertiesSet() throws SQLException {
        try (Connection connection = dataSource.getConnection()) {
            Map<String, String> rewritten = new LinkedHashMap<>();
            try (Statement statement = connection.createStatement();
                 ResultSet tables = statement.executeQuery("select name, sql from sqlite_master where type = 'table' and sql like '%check%'")) {
                while (tables.next()) {
                    String sql = tables.getString("sql");
                    String cleaned = ENUM_CHECK.matcher(sql).replaceAll("");
                    if (!cleaned.equals(sql)) {
                        rewritten.put(tables.getString("name"), cleaned);
                    }
                }
            }
            if (rewritten.isEmpty()) {
                return;
            }
            try (Statement statement = connection.createStatement()) {
                statement.execute("pragma foreign_keys = off");
            }
            boolean autoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);
            try (Statement statement = connection.createStatement()) {
                for (Map.Entry<String, String> table : rewritten.entrySet()) {
                    rebuild(connection, statement, table.getKey(), table.getValue());
                }
                connection.commit();
                LOG.info("Removed stale enum checks from tables {}.", rewritten.keySet());
            } catch (SQLException e) {
                connection.rollback();
                throw e;
            } finally {
                connection.setAutoCommit(autoCommit);
                try (Statement statement = connection.createStatement()) {
                    statement.execute("pragma foreign_keys = on");
                }
            }
        }
    }

    /** SQLite's table rebuild: new table with the cleaned definition, copy, drop, rename, indexes back. */
    private static void rebuild(Connection connection, Statement statement, String table, String cleanedSql) throws SQLException {
        String quoted = '"' + table.replace("\"", "\"\"") + '"';
        String temporary = '"' + table.replace("\"", "\"\"") + "__rebuild\"";
        List<String> indexes = new ArrayList<>();
        try (PreparedStatement query = connection.prepareStatement(
            "select sql from sqlite_master where type = 'index' and tbl_name = ? and sql is not null")) {
            query.setString(1, table);
            try (ResultSet result = query.executeQuery()) {
                while (result.next()) {
                    indexes.add(result.getString(1));
                }
            }
        }
        String createTemporary = CREATE_TABLE_NAME.matcher(cleanedSql).replaceFirst(Matcher.quoteReplacement("CREATE TABLE " + temporary));
        statement.execute(createTemporary);
        statement.execute("insert into " + temporary + " select * from " + quoted);
        statement.execute("drop table " + quoted);
        statement.execute("alter table " + temporary + " rename to " + quoted);
        for (String index : indexes) {
            statement.execute(index);
        }
    }

    /** Makes the JPA EntityManagerFactory wait for the cleanup. */
    @Configuration
    static class RunBeforeJpa extends EntityManagerFactoryDependsOnPostProcessor {
        RunBeforeJpa() {
            super(EnumCheckCleaner.class);
        }
    }
}
