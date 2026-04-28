package com.example.supermarket2.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    /**
     * 配置全局 OpenAPI 信息（标题、版本、JWT 认证等）
     */
    @Bean
    public OpenAPI customOpenAPI() {
        // 1. 定义 JWT 安全认证方案
        SecurityScheme jwtScheme = new SecurityScheme()
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT")
                .name("BearerAuth");

        // 2. 全局应用 JWT 认证（所有接口默认需要 Token）
        SecurityRequirement securityRequirement = new SecurityRequirement().addList("BearerAuth");

        // 3. 构建文档元信息
        return new OpenAPI()
                .info(new Info()
                        .title("Supermarket API")       // 文档标题
                        .version("1.0")                // 版本号
                        .description("超市商城接口文档") // 描述
                        .contact(new Contact()
                                .name("Developer")
                                .email("dev@example.com"))) // 作者信息
                .addSecurityItem(securityRequirement) // 全局开启 JWT 认证
                .components(new io.swagger.v3.oas.models.Components()
                        .addSecuritySchemes("BearerAuth", jwtScheme)); // 注册 JWT 安全方案
    }

    /**
     * 配置「admin 模块」接口分组（扫描指定包下的 Controller）
     */
    @Bean
    public GroupedOpenApi adminApi() {
        return GroupedOpenApi.builder()
                .group("admin") // 分组名称（左侧导航显示）
                .packagesToScan("com.example.supermarket2.controller.admin") // Controller 包路径
                .pathsToMatch("/admin/**") // 接口路径匹配规则
                .build();
    }

    /**
     * 配置「app 模块」接口分组
     */
    @Bean
    public GroupedOpenApi appApi() {
        return GroupedOpenApi.builder()
                .group("app")
                .packagesToScan("com.example.supermarket2.controller.app")
                .pathsToMatch("/app/**")
                .build();
    }
}