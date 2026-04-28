package com.example.supermarket2.handler;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedJdbcTypes;
import org.apache.ibatis.type.MappedTypes;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

/**
 * List<String>与JSON字符串类型转换器
 * 修复点：
 * 1. 去掉 @Component/@RequiredArgsConstructor，MyBatis 不通过 Spring 实例化
 * 2. 显式添加 @NoArgsConstructor 无参构造
 * 3. ObjectMapper 自己 new 一个（不要依赖 Spring 注入）
 */
@Slf4j
@NoArgsConstructor // 【关键修复1】显式声明无参构造函数
@MappedTypes(List.class)
@MappedJdbcTypes(JdbcType.VARCHAR)
public class ListStringTypeHandler extends BaseTypeHandler<List<String>> {

    // 【关键修复2】自己 new ObjectMapper，不要依赖 Spring 注入
    // 如果需要和 Spring 的全局 ObjectMapper 配置一致，可以通过静态方法获取，或者这里手动配置相同的规则
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private static final TypeReference<List<String>> LIST_TYPE_REFERENCE = new TypeReference<List<String>>() {};

    // 插入数据库时：将List<String>转为JSON字符串
    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, List<String> parameter, JdbcType jdbcType) throws SQLException {
        try {
            String json = objectMapper.writeValueAsString(parameter);
            ps.setString(i, json);
        } catch (Exception e) {
            log.error("List<String>转JSON失败", e);
            throw new SQLException("List<String>转JSON失败", e);
        }
    }

    // 查询时：从数据库JSON字符串转为List<String>
    @Override
    public List<String> getNullableResult(ResultSet rs, String columnName) throws SQLException {
        return parseJson(rs.getString(columnName));
    }

    @Override
    public List<String> getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        return parseJson(rs.getString(columnIndex));
    }

    @Override
    public List<String> getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        return parseJson(cs.getString(columnIndex));
    }

    // 解析JSON字符串为List<String>
    private List<String> parseJson(String json) {
        if (json == null || json.trim().isEmpty()) {
            return null;
        }
        try {
            return objectMapper.readValue(json, LIST_TYPE_REFERENCE);
        } catch (Exception e) {
            log.error("JSON转List<String>失败: json={}", json, e);
            throw new RuntimeException("JSON转List<String>失败: " + json, e);
        }
    }
}