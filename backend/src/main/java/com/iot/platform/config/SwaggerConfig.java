package com.iot.platform.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * SpringDoc OpenAPI 3 配置 — 自动生成REST API文档
 * <p>
 * 访问地址：http://localhost:8081/doc.html (Knife4j增强UI)
 * 原生地址：http://localhost:8081/swagger-ui.html
 *
 * @author 王恒
 */
@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("智能物联设备管理平台 API 文档")
                        .description("IoT Device Management Platform — 设备接入/影子/告警/OTA/产品管理")
                        .version("V1.0")
                        .contact(new Contact()
                                .name("王恒")
                                .email("535698505@qq.com")))
                .addSecurityItem(new SecurityRequirement().addList("Bearer"))
                .schemaRequirement("Bearer", new SecurityScheme()
                        .name("Authorization")
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")
                        .description("登录后获取的JWT Token"));
    }
}
