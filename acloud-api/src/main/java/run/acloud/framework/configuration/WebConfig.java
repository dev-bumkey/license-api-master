package run.acloud.framework.configuration;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.joda.JodaModule;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.ByteArrayHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.async.DeferredResult;
import org.springframework.web.context.request.async.DeferredResultProcessingInterceptor;
import org.springframework.web.multipart.MultipartResolver;
import org.springframework.web.multipart.support.StandardServletMultipartResolver;
import org.springframework.web.servlet.config.annotation.*;
import org.springframework.web.servlet.resource.WebJarsResourceResolver;
import run.acloud.api.resource.util.JavaTimeModuleUtils;
import run.acloud.framework.enums.AuthExcludePathPattern;
import run.acloud.framework.interceptor.AuthHandlerInterceptor;
import run.acloud.framework.interceptor.HttpMethodRestrictsInterceptor;

import java.util.List;

/**
 * @author hjchoi
 */
@Configuration
public class WebConfig extends WebMvcConfigurationSupport {

    @Bean
    public MappingJackson2HttpMessageConverter customJackson2HttpMessageConverter() {
        MappingJackson2HttpMessageConverter jsonConverter = new MappingJackson2HttpMessageConverter();

        ObjectMapper objectMapper = new ObjectMapper();

        objectMapper.registerModule(new JodaModule());
        objectMapper.registerModule(JavaTimeModuleUtils.getModule());
        // Deserialization
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        objectMapper.disable(DeserializationFeature.ADJUST_DATES_TO_CONTEXT_TIME_ZONE);
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        // Serialization
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        jsonConverter.setObjectMapper(objectMapper);

        return jsonConverter;
    }

    @Override
    public void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
        converters.add(new ByteArrayHttpMessageConverter());
        converters.add(customJackson2HttpMessageConverter());
        super.configureMessageConverters(converters);
    }

    @Override
    public void configureContentNegotiation(ContentNegotiationConfigurer configurer) {
        configurer.favorPathExtension(false);
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry
                .addResourceHandler("/webjars/**")
                .addResourceLocations("classpath:/META-INF/resources/webjars/")
                .resourceChain(true)
                .addResolver(new WebJarsResourceResolver())
        ;
        registry
                .addResourceHandler("/**")
                .addResourceLocations("classpath:/META-INF/resources/")
                .addResourceLocations("classpath:/resources/")
                .addResourceLocations("classpath:/static/")
                .addResourceLocations("classpath:/public/")
        ;
        registry
                .addResourceHandler("/swagger-ui*/*swagger-initializer.js")
                .addResourceLocations("classpath:/META-INF/resources/webjars/")
                .resourceChain(true)
                .addResolver(new WebJarsResourceResolver())
        ;
        registry
                .addResourceHandler("/swagger-ui*/**")
                .addResourceLocations("classpath:/META-INF/resources/webjars/")
                .resourceChain(true)
                .addResolver(new WebJarsResourceResolver())
        ;
//        registry
//                .addResourceHandler("/*.html","/*.js","/*.css")
//                .addResourceLocations("classpath:/static/");
    }

    @Override
    protected void configureAsyncSupport(AsyncSupportConfigurer configurer) {
        configurer.setDefaultTimeout(1800000);
        configurer.registerDeferredResultInterceptors(
                new DeferredResultProcessingInterceptor() {
                    @Override
                    public <T> boolean handleTimeout(NativeWebRequest req, DeferredResult<T> result) {
                        return result.setErrorResult(new AsyncTimeoutException());
                    }
                });
    }

    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    public static class AsyncTimeoutException extends Exception {
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {

        // http method restricts
        registry.addInterceptor(new HttpMethodRestrictsInterceptor())
                .addPathPatterns("/**");

        // TODO: excludePath 처리 고민 필요
        InterceptorRegistration authRegistration = registry.addInterceptor(new AuthHandlerInterceptor());
        authRegistration.addPathPatterns("/api/**");
        String[] excludePathPatterns = AuthExcludePathPattern.toArray();
        if (ArrayUtils.isNotEmpty(excludePathPatterns)) {
            authRegistration.excludePathPatterns(excludePathPatterns);
        }

    }

    @Bean
    public MultipartResolver multipartResolver() {
        return new StandardServletMultipartResolver();
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
//                .allowedOrigins("*")
//                .allowedHeaders("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE");
    }
}
