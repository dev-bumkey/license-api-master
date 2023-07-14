package run.acloud.framework.configuration;

import org.apache.tomcat.jdbc.pool.DataSource;
import org.springframework.context.annotation.Bean;
import run.acloud.framework.properties.IDatabaseProperties;

public abstract class DatabaseConfig {
    
    @Bean
    public abstract DataSource dataSource();
  
    void configureDataSource(DataSource dataSource, IDatabaseProperties databaseProperties) {
        dataSource.setDriverClassName(databaseProperties.getDriverClassName());
        dataSource.setUrl(databaseProperties.getUrl());
        dataSource.setUsername(databaseProperties.getUserName());
        dataSource.setPassword(databaseProperties.getPassword());
        dataSource.setMaxActive(databaseProperties.getMaxActive());
        dataSource.setMaxIdle(databaseProperties.getMaxIdle());
        dataSource.setMinIdle(databaseProperties.getMinIdle());
        dataSource.setTestOnBorrow(true);
        dataSource.setValidationQuery("SELECT 1");
    }
}
