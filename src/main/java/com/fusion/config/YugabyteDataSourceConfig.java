package com.fusion.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

@Configuration
public class YugabyteDataSourceConfig {

    // Inject database connection properties from application.properties
    @Value("${yugabyte.datasource.url}")
    private String jdbcUrl;

    @Value("${yugabyte.datasource.username}")
    private String username;

    @Value("${yugabyte.datasource.password}")
    private String password;

    @Value("${yugabyte.datasource.driver-class-name}")
    private String driverClassName;

    // Inject connection pool settings
    @Value("${yugabyte.datasource.max-pool-size}")
    private int maxPoolSize;

    @Value("${yugabyte.datasource.min-idle}")
    private int minIdle;

    @Value("${yugabyte.datasource.idle-timeout}")
    private long idleTimeout;

    @Value("${yugabyte.datasource.connection-timeout}")
    private long connectionTimeout;

    @Value("${yugabyte.datasource.max-lifetime}")
    private long maxLifetime;

    @Bean
    public DataSource dataSource() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(jdbcUrl);
        config.setUsername(username);
        config.setPassword(password);
        config.setDriverClassName(driverClassName);

        config.setMaximumPoolSize(maxPoolSize);
        config.setMinimumIdle(minIdle);
        config.setIdleTimeout(idleTimeout);
        config.setConnectionTimeout(connectionTimeout);
        config.setMaxLifetime(maxLifetime);

        return new HikariDataSource(config);
    }
}