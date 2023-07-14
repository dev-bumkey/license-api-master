package run.acloud.framework.configuration;

import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.type.TypeHandler;
import org.apache.tomcat.jdbc.pool.DataSource;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import run.acloud.framework.handlers.YesNoBooleanTypeHandler;
import run.acloud.framework.properties.CocktailDBProperties;

/**
 * @author vancheju on 17. 5. 19.
 */
@Configuration
@EnableAutoConfiguration(exclude = {DataSourceTransactionManagerAutoConfiguration.class, DataSourceAutoConfiguration.class})
@EnableTransactionManagement(proxyTargetClass = true)
@EnableConfigurationProperties({CocktailDBProperties.class})
public class DefaultDatabaseConfig extends DatabaseConfig {

    private final ApplicationContext applicationContext;
    private final CocktailDBProperties cocktailDBProperties;

    @Autowired
    public DefaultDatabaseConfig(ApplicationContext applicationContext, CocktailDBProperties cocktailDBProperties) {
        this.applicationContext = applicationContext;
        this.cocktailDBProperties = cocktailDBProperties;
    }

    @Primary
    @Bean(name = "dataSource", destroyMethod = "close")
    public DataSource dataSource() {
        org.apache.tomcat.jdbc.pool.DataSource dataSource = new org.apache.tomcat.jdbc.pool.DataSource();
        configureDataSource(dataSource, cocktailDBProperties);
        return dataSource;
    }

    @Primary
    @Bean(name = "transactionManager")
    public PlatformTransactionManager transactionManager(@Qualifier("dataSource") DataSource dataSource) {
        DataSourceTransactionManager transactionManager = new DataSourceTransactionManager(dataSource);
        transactionManager.setGlobalRollbackOnParticipationFailure(false);
        return transactionManager;
    }

    @Bean(name = "sqlSessionFactory")
    public SqlSessionFactory sqlSessionFactory(@Qualifier("dataSource") DataSource dataSource) throws Exception {
        SqlSessionFactoryBean sessionFactoryBean = new SqlSessionFactoryBean();
        sessionFactoryBean.setDataSource(dataSource);
        sessionFactoryBean.setTypeAliasesPackage("com.stunstun.spring.repository.entity");
        sessionFactoryBean.setConfigLocation(applicationContext.getResource("classpath:run/acloud/api/mybatis-config.xml"));
        sessionFactoryBean.setMapperLocations(applicationContext.getResources("classpath:run/acloud/api/**/**/mapper/*.xml"));

        // Custom TypeHandlers
        sessionFactoryBean.setTypeHandlersPackage("run.acloud.framework.handlers");
        sessionFactoryBean.setTypeHandlers(new TypeHandler[]{
                new YesNoBooleanTypeHandler()
        });
        return sessionFactoryBean.getObject();
    }

    @Bean(name = "cocktailSession")
    public SqlSessionTemplate sqlSession(@Qualifier("sqlSessionFactory") SqlSessionFactory sqlSessionFactory) throws Exception {
        return new SqlSessionTemplate(sqlSessionFactory);
    }

//    /**
//     * SpringBatch에서 DataSource 타입을 찾아 사용하는데, 현재 API와 BUILD용 두 개의 DataSource를 사용해서 배치설정시 오류 발생함.
//     * 현재 배치에서 사용하는 DB는 API DB만 사용하므로 dataSource를 이용해 BatchConfigurer Bean 생성해주는 메서드를 만들어주면 해결 된다
//     *
//     * @return BatchConfigurer
//     */
//    @Bean
//    public BatchConfigurer batchConfig() {
//        return new DefaultBatchConfigurer(dataSource());
//    }

}
