package com.example.supermarket2.config;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.TimeZone;

/**
 * Jackson全局配置
 * 全项目唯一ObjectMapper单例入口，确保所有场景序列化规则统一
 * 包括：Web请求、Redis存储、MyBatis类型转换、MQTT消息解析
 */
@Configuration
public class JacksonConfig {
    // 日期时间格式
    public static final String DEFAULT_DATE_TIME_FORMAT = "yyyy-MM-dd HH:mm:ss";
    // 日期格式
    public static final String DEFAULT_DATE_FORMAT = "yyyy-MM-dd";
    // 时区
    public static final String DEFAULT_TIME_ZONE = "GMT+8";

    /**
     * 全局唯一ObjectMapper Bean，@Primary确保优先注入
     * 所有需要ObjectMapper的场景，必须通过@Autowired注入此Bean，禁止new ObjectMapper()
     */
    @Bean
    @Primary
    public ObjectMapper objectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();

        // 1. 配置全局日期时间格式化
        // 处理java.util.Date类型
        SimpleDateFormat dateFormat = new SimpleDateFormat(DEFAULT_DATE_TIME_FORMAT);
        dateFormat.setTimeZone(TimeZone.getTimeZone(DEFAULT_TIME_ZONE));
        objectMapper.setDateFormat(dateFormat);

        // 处理Java8时间类型（LocalDateTime、LocalDate）
        JavaTimeModule javaTimeModule = new JavaTimeModule();
        // 序列化LocalDateTime
        javaTimeModule.addSerializer(LocalDateTime.class, new LocalDateTimeSerializer(
                DateTimeFormatter.ofPattern(DEFAULT_DATE_TIME_FORMAT)));
        // 反序列化LocalDateTime
        javaTimeModule.addDeserializer(LocalDateTime.class, new LocalDateTimeDeserializer(
                DateTimeFormatter.ofPattern(DEFAULT_DATE_TIME_FORMAT)));
        // 序列化LocalDate
        javaTimeModule.addSerializer(LocalDate.class, new LocalDateSerializer(
                DateTimeFormatter.ofPattern(DEFAULT_DATE_FORMAT)));
        // 反序列化LocalDate
        javaTimeModule.addDeserializer(LocalDate.class, new LocalDateDeserializer(
                DateTimeFormatter.ofPattern(DEFAULT_DATE_FORMAT)));
        objectMapper.registerModule(javaTimeModule);

        // 2. 配置序列化策略
        // 忽略值为null的字段（生产环境推荐，减少报文体积）
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        // 允许序列化空的POJO类（否则会抛出异常）
        objectMapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
        // 序列化枚举时使用枚举的toString()方法
        objectMapper.enable(SerializationFeature.WRITE_ENUMS_USING_TO_STRING);
        // 禁用日期序列化为时间戳
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        // 3. 配置反序列化策略
        // 忽略JSON中存在但Java对象不存在的字段（前后端兼容必备）
        objectMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        // 允许将空字符串转换为null
        objectMapper.enable(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT);
        // 允许单引号（默认只支持双引号）
        objectMapper.enable(JsonParser.Feature.ALLOW_SINGLE_QUOTES);
        // 允许JSON字段名没有引号（非标准JSON）
        objectMapper.enable(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES);
        // 允许数字前导零
        objectMapper.enable(JsonParser.Feature.ALLOW_NUMERIC_LEADING_ZEROS);

        // 4. 自定义反序列化器：字符串trim处理（去除前后空格）
        javaTimeModule.addDeserializer(String.class, new com.fasterxml.jackson.databind.JsonDeserializer<String>() {
            @Override
            public String deserialize(com.fasterxml.jackson.core.JsonParser p, com.fasterxml.jackson.databind.DeserializationContext ctxt) throws IOException {
                String value = p.getValueAsString();
                return StringUtils.hasText(value) ? value.trim() : null;
            }
        });

        return objectMapper;
    }
}