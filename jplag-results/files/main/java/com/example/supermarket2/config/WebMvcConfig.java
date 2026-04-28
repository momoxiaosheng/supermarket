package com.example.supermarket2.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private static final Logger log = LoggerFactory.getLogger(WebMvcConfig.class);

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        log.info("===== 开始配置 Knife4j 静态资源映射 =====");
        // 1. 映射 doc.html 到 classpath:/META-INF/resources/
        registry.addResourceHandler("doc.html")
                .addResourceLocations("classpath:/META-INF/resources/");
        // 2. 映射 webjars 到 classpath:/META-INF/resources/webjars/
        registry.addResourceHandler("/webjars/**")
                .addResourceLocations("classpath:/META-INF/resources/webjars/");
        log.info("===== Knife4j 静态资源映射配置完成 =====");

        registry.addResourceHandler("/images/**")
                .addResourceLocations("classpath:/static/images/");
        log.info("===== 用户图片静态资源映射配置完成 =====");
    }
}