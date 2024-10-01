package com.fusion;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

public class YugabyteConnectionPool {

    private static HikariDataSource dataSource;

    static {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:postgresql://localhost:5433/postgres");
        config.setUsername("yugabyte");  // Default Yugabyte user
        config.setPassword("yugabyte");  // Default password (empty if none)

        // Use PostgreSQL driver class (for Yugabyte compatibility)
        config.setDriverClassName("org.postgresql.Driver");

        // HikariCP settings - tune them as per your requirements
        config.setMaximumPoolSize(10);
        config.setConnectionTimeout(30000);
        config.setIdleTimeout(600000);
        config.setMaxLifetime(1800000);

        // Other optional settings
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");

        // Initialize the datasource with HikariConfig
        dataSource = new HikariDataSource(config);
    }

    // Method to get a connection from the pool
    public static Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    // Method to close the pool
    public static void closePool() {
        if (dataSource != null) {
            dataSource.close();
        }
    }
}