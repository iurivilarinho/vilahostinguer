package com.bancada.config;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.sqlite.SQLiteDataSource;

class EnumCheckCleanerTest {

    @TempDir
    Path directory;

    @Test
    void newEnumValueIsAcceptedAfterTheCleanupAndOtherChecksStay() throws SQLException {
        SQLiteDataSource dataSource = new SQLiteDataSource();
        dataSource.setUrl("jdbc:sqlite:" + directory.resolve("test.db"));
        try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement()) {
            statement.execute("create table operations (id integer, size integer check (size >= 0), "
                + "type varchar(255) not null check (type in ('INSTALL_APP','BACKUP')), primary key (id))");
            statement.execute("insert into operations (type, size) values ('BACKUP', 1)");
            assertThrows(SQLException.class, () -> statement.execute("insert into operations (type, size) values ('MACHINE_CREATE', 1)"));
        }

        new EnumCheckCleaner(dataSource).afterPropertiesSet();

        try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement()) {
            assertDoesNotThrow(() -> statement.execute("insert into operations (type, size) values ('MACHINE_CREATE', 1)"));
            assertThrows(SQLException.class, () -> statement.execute("insert into operations (type, size) values ('BACKUP', -1)"));
            try (ResultSet result = statement.executeQuery("select sql from sqlite_master where name = 'operations'")) {
                String sql = result.getString(1);
                assertFalse(sql.contains("INSTALL_APP"));
                assertTrue(sql.contains("not null"));
            }
            try (ResultSet result = statement.executeQuery("select count(*) from operations")) {
                assertEquals(2, result.getInt(1));
            }
        }
    }
}
