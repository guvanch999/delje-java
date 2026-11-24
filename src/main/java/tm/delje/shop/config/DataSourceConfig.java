package tm.delje.shop.config;

import com.zaxxer.hikari.HikariDataSource;
import javax.sql.DataSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DataSourceConfig {

    @Bean
    public DataSource dataSource(AppProperties properties) {
        DatabaseUrl db = DatabaseUrl.parse(properties.getDatabaseUrl());

        HikariDataSource dataSource = new HikariDataSource();
        dataSource.setJdbcUrl(db.jdbcUrl());
        dataSource.setUsername(db.username());
        dataSource.setPassword(db.password());
        dataSource.setMaximumPoolSize(10);
        dataSource.setPoolName("delje-pool");
        return dataSource;
    }
}
