package run.acloud.framework.configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.parameters.HeaderParameter;
import io.swagger.v3.oas.models.servers.Server;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Value("${springdoc.version}")
    private String version;

    @Bean
    public OpenAPI openAPI() {

        return new OpenAPI()
                .addServersItem(new Server().url("/"))
                .components(new Components())
                .info(this.getInfo());
    }

//    @Bean
//    public GroupedOpenApi auditGroup() {
//        return GroupedOpenApi.builder().group("audit")
//                .addOpenApiCustomizer(this.openApiCustomizer())
//                .packagesToScan("run.acloud.api.audit")
//                .build();
//    }

    private Info getInfo() {
        Info info = new Info()
                .title("Cocktail API") // 타이틀
                .version(version) // 문서 버전
                .description("API Documentation for Cocktail") // 문서 설명
                .contact(new Contact() // 연락처
                        .name("cocktail")
                        .email("cocktail@acornsoft.io")
                        .url("https://www.cocktailcloud.io/"));

        return info;
    }


  @Bean
  public OpenApiCustomizer openApiCustomizer() {

    return OpenApi -> OpenApi
            .addServersItem(getServersItem())
            .info(this.getInfo())
            .getPaths().values().stream()
            .flatMap(pathItem -> pathItem.readOperations().stream())
            .forEach(operation -> operation
                    .addParametersItem(new HeaderParameter()
                            .name("user-id")
                            .description("user-id")
                            .required(Boolean.FALSE)
                            .schema(new StringSchema()))
                    .addParametersItem(new HeaderParameter()
                            .name("user-role")
                            .description("user-role")
                            .required(Boolean.FALSE)
                            .schema(new StringSchema()))
                    .addParametersItem(new HeaderParameter()
                            .name("user-workspace")
                            .description("user-workspace")
                            .required(Boolean.FALSE)
                            .schema(new StringSchema()))
                    .addParametersItem(new HeaderParameter()
                            .name("user-grant")
                            .description("user-grant")
                            .required(Boolean.FALSE)
                            .schema(new StringSchema()))
            )
            ;

  }

  private Server getServersItem() {
    return new Server().url("/");
  }

//  @Bean
//  public InternalResourceViewResolver defaultViewResolver() {
//    return new InternalResourceViewResolver();
//  }
}