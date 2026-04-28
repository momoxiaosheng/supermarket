package com.example.supermarket2.config;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.TimeZone;

@Configuration
public class JacksonConfig {

    // 日期时间格式
    public static final String DEFAULT_DATE_TIME_FORMAT = "yyyy-MM-dd HH:mm:ss";
    // 日期格式
    public static final String DEFAULT_DATE_FORMAT = "yyyy-MM-dd";

    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();

        // 1. 配置全局日期时间格式化
        // 处理java.util.Date类型
        SimpleDateFormat dateFormat = new SimpleDateFormat(DEFAULT_DATE_TIME_FORMAT);
        dateFormat.setTimeZone(TimeZone.getTimeZone("GMT+8")); // 设置时区为东八区
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
        // 忽略值为null的字段
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        // 忽略值为默认值的字段（如int默认0，boolean默认false）
        objectMapper.setDefaultPropertyInclusion(JsonInclude.Include.NON_DEFAULT);
        // 允许序列化空的POJO类（否则会抛出异常）
        objectMapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
        // 序列化枚举时使用枚举的toString()方法
        objectMapper.enable(SerializationFeature.WRITE_ENUMS_USING_TO_STRING);

        // 3. 配置反序列化策略
        // 忽略JSON中存在但Java对象不存在的字段
        objectMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        // 允许将空字符串转换为null
        objectMapper.enable(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT);
        // 允许单引号（默认只支持双引号）
        objectMapper.enable(JsonParser.Feature.ALLOW_SINGLE_QUOTES);
        // 允许JSON字段名没有引号（非标准JSON）
        objectMapper.enable(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES);

        // 4. 自定义序列化器/反序列化器示例
        SimpleModule customModule = new SimpleModule();
        // 示例：字符串trim处理（去除前后空格）
        customModule.addDeserializer(String.class, new JsonDeserializer<String>() {
            @Override
            public String deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
                String value = p.getValueAsString();
                return StringUtils.hasText(value) ? value.trim() : null;
            }
        });
        // 示例：序列化BigDecimal时避免科学计数法
        /*customModule.addSerializer(BigDecimal.class, new JsonSerializer<BigDecimal>() {
            @Override
            public void serialize(BigDecimal value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
                if (value != null) {
                    gen.writeString(value.toPlainString());
                }
            }
        });*/
        objectMapper.registerModule(customModule);

        return objectMapper;
    }
}

