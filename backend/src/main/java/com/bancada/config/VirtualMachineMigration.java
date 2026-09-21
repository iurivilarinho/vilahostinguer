package com.bancada.config;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
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
 * Machines used to be Docker containers on the devices; now they are Hyper-V virtual machines on
 * this PC. The old tables cannot be reused ({@code ddl-auto: update} never drops a NOT NULL column
 * such as the container name), so, once, before Hibernate starts:
 * <ul>
 *   <li>the container machines and what points at them go away: their routes and whole-machine
 *   backups (the files stay on disk), and the links from subscriptions and disks;</li>
 *   <li>plans lose the required device (a plan now runs on this PC);</li>
 *   <li>subscriptions of distributions no longer offered move to Ubuntu (they only record the choice).</li>
 * </ul>
 */
@Component
public class VirtualMachineMigration implements InitializingBean {

    private static final Logger LOG = LoggerFactory.getLogger(VirtualMachineMigration.class);
    private static final Pattern PLAN_DEVICE_NOT_NULL = Pattern.compile("(fk_Id_Device\\s+\\w+)\\s+not\\s+null", Pattern.CASE_INSENSITIVE);
    private static final List<String> DISTRIBUTIONS = List.of("UBUNTU", "DEBIAN", "ROCKY", "ALMA");

    private final DataSource dataSource;

    public VirtualMachineMigration(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void afterPropertiesSet() throws SQLException {
        try (Connection connection = dataSource.getConnection()) {
            boolean containerMachines = hasColumn(connection, "machines", "container_name");
            String planSql = tableSql(connection, "plans");
            boolean planDeviceRequired = planSql != null && PLAN_DEVICE_NOT_NULL.matcher(planSql).find();
            if (!containerMachines && !planDeviceRequired) {
                return;
            }
            try (Statement statement = connection.createStatement()) {
                statement.execute("pragma foreign_keys = off");
            }
            boolean autoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);
            try (Statement statement = connection.createStatement()) {
                if (containerMachines) {
                    executeIfTable(connection, statement, "routes", "delete from routes where fk_Id_Machine is not null");
                    executeIfTable(connection, statement, "backups", "delete from backups where fk_Id_Machine is not null");
                    executeIfTable(connection, statement, "subscriptions", "update subscriptions set fk_Id_Machine = null");
                    executeIfTable(connection, statement, "volumes", "update volumes set fk_Id_Machine = null, container_path = null");
                    String offered = String.join("','", DISTRIBUTIONS);
                    executeIfTable(connection, statement, "subscriptions",
                        "update subscriptions set distribution = 'UBUNTU', version = '24.04' where distribution not in ('" + offered + "')");
                    statement.execute("drop table if exists machine_ports");
                    statement.execute("drop table if exists machine_volumes");
                    statement.execute("drop table if exists machines");
                }
                if (planDeviceRequired) {
                    String relaxed = PLAN_DEVICE_NOT_NULL.matcher(planSql).replaceFirst("$1");
                    statement.execute(relaxed.replaceFirst("(?i)^CREATE\\s+TABLE\\s+\\S+", Matcher.quoteReplacement("CREATE TABLE plans__rebuild")));
                    statement.execute("insert into plans__rebuild select * from plans");
                    statement.execute("drop table plans");
                    statement.execute("alter table plans__rebuild rename to plans");
                }
                connection.commit();
                LOG.info("Moved to virtual machines on this PC (container machines removed: {}, plan device optional: {}).",
                    containerMachines, planDeviceRequired);
            } catch (SQLException exception) {
                connection.rollback();
                throw exception;
            } finally {
                connection.setAutoCommit(autoCommit);
                try (Statement statement = connection.createStatement()) {
                    statement.execute("pragma foreign_keys = on");
                }
            }
        }
    }

    private static boolean hasColumn(Connection connection, String table, String column) throws SQLException {
        try (Statement statement = connection.createStatement();
             ResultSet columns = statement.executeQuery("pragma table_info(" + table + ")")) {
            while (columns.next()) {
                if (column.equalsIgnoreCase(columns.getString("name"))) {
                    return true;
                }
            }
        }
        return false;
    }

    private static String tableSql(Connection connection, String table) throws SQLException {
        try (Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery("select sql from sqlite_master where type = 'table' and name = '" + table + "'")) {
            return result.next() ? result.getString(1) : null;
        }
    }

    private static void executeIfTable(Connection connection, Statement statement, String table, String sql) throws SQLException {
        if (tableSql(connection, table) != null) {
            statement.execute(sql);
        }
    }

    /** Makes the JPA EntityManagerFactory wait for the migration. */
    @Configuration
    static class RunBeforeJpa extends EntityManagerFactoryDependsOnPostProcessor {
        RunBeforeJpa() {
            super(VirtualMachineMigration.class);
        }
    }
}
