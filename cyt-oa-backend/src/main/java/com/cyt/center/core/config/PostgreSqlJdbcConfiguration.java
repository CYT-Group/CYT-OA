package com.cyt.center.core.config;

import org.springframework.boot.autoconfigure.jdbc.JdbcProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementSetter;

import javax.sql.DataSource;
import java.util.Arrays;

/** JDBC 与 MyBatis 共同遵守 SMALLINT 开关列的 0/1 存储契约。 */
@Configuration
public class PostgreSqlJdbcConfiguration {
    @Bean
    public JdbcTemplate jdbcTemplate(DataSource dataSource, JdbcProperties properties) {
        var jdbc = new NumericBooleanJdbcTemplate(dataSource);
        jdbc.setFetchSize(properties.getTemplate().getFetchSize());
        jdbc.setMaxRows(properties.getTemplate().getMaxRows());
        var timeout = properties.getTemplate().getQueryTimeout();
        if (timeout != null) jdbc.setQueryTimeout((int) timeout.getSeconds());
        return jdbc;
    }

    public static Object databaseValue(Object value) {
        return value instanceof Boolean flag ? (flag ? 1 : 0) : value;
    }

    public static class NumericBooleanJdbcTemplate extends JdbcTemplate {
        public NumericBooleanJdbcTemplate(DataSource source) {
            super(source);
        }

        @Override
        protected PreparedStatementSetter newArgPreparedStatementSetter(Object[] args) {
            return super.newArgPreparedStatementSetter(args == null ? null :
                    Arrays.stream(args).map(PostgreSqlJdbcConfiguration::databaseValue).toArray());
        }
    }
}
