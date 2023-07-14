package run.acloud;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import run.acloud.framework.filter.*;

/**
 * Spring-boot 기반의 Web/REST Api Application 클레스
 *
 * @author winto@acornsoft.io
 * @since 2017. 01. 08.
 *
 */
@Slf4j
@Configuration
@EnableAsync(proxyTargetClass=true)
@EnableCaching
@SpringBootApplication(scanBasePackages = {"run.acloud"}, exclude={SecurityAutoConfiguration.class})
public class AcloudApiApplication implements ApplicationRunner {


    /**
     * MultiReadableRequestBodyFilter Class 필터를 추가 등록하고 우선순위를 1번째로 지정 (2018.09.06)
     * @return FilterRegistrationBean
     */
    @Bean
    public FilterRegistrationBean getMultiReadableRequestBodyFilter()
    {
        FilterRegistrationBean registration = new FilterRegistrationBean();
        registration.setFilter(new MultiReadableRequestBodyFilter());
        registration.setOrder(1);
        return registration;
    }

    /**
     * TransactionLoggingFilter Class 필터를 추가 등록하고 우선순위를 1번째로 지정 (2018.09.06)
     * @return FilterRegistrationBean
     */
    @Bean
    public FilterRegistrationBean getTransactionLoggingFilter()
    {
        FilterRegistrationBean registration = new FilterRegistrationBean();
        registration.setFilter(new TransactionLoggingFilter());
        registration.setOrder(2);
        return registration;
    }

    /**
     * 기 사용중이던 LoggerFilter Class 필터 등록 (2018.09.06)
     * @return FilterRegistrationBean
     */
    @Bean
	public FilterRegistrationBean getLoggerFilter()
	{
	    FilterRegistrationBean registration = new FilterRegistrationBean();
	    registration.setFilter(new LoggerFilter());
	    registration.setOrder(10);
	    return registration;
	}

    /**
     * InternalAuthFilter Class 필터 등록
     * @return FilterRegistrationBean
     */
    @Bean
	public FilterRegistrationBean getInternalAuthFilter()
	{
	    FilterRegistrationBean registration = new FilterRegistrationBean();
	    registration.setFilter(new InternalAuthFilter());
	    registration.setOrder(12);
	    return registration;
	}

    /**
     * InternalAuthFilter Class 필터 등록
     * @return FilterRegistrationBean
     */
    @Bean
	public FilterRegistrationBean getOpenApiDocsFormatterFilter()
	{
	    FilterRegistrationBean registration = new FilterRegistrationBean();
	    registration.setFilter(new OpenApiDocsFormatterFilter());
		registration.addUrlPatterns("/v3/api-docs/*");
	    registration.setOrder(15);
	    return registration;
	}

	public static void main(String[] args) {
		String springDevtoolsRestartEnabled = "false";
		for (String arg : args) {
			if (arg != null && arg.startsWith("--SPRING_DEVTOOLS_RESTART_ENABLED=")) {
				springDevtoolsRestartEnabled = arg.split("=")[1];
				break;
			}
		}
		System.setProperty("spring.devtools.restart.enabled", springDevtoolsRestartEnabled);
		SpringApplication.run(AcloudApiApplication.class, args);
	}

    @Override
    public void run(ApplicationArguments args) throws Exception {

    }
}
