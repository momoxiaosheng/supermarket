package com.example.supermarket2.handler;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

@Component
public class ListStringTypeHandler extends BaseTypeHandler<List<String>> {

    // 复用项目中已配置的ObjectMapper（包含日期格式化等配置）
    private ObjectMapper objectMapper;

    // 无参构造函数，供MyBatis实例化使用
    public ListStringTypeHandler() {
        this.objectMapper = new ObjectMapper();
    }

    // Spring依赖注入构造函数
    @Autowired
    public ListStringTypeHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    // 插入数据库时：将List<String>转为JSON字符串
    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, List<String> parameter, JdbcType jdbcType) throws SQLException {
        try {
            String json = objectMapper.writeValueAsString(parameter);
            ps.setString(i, json);
        } catch (Exception e) {
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
            return objectMapper.readValue(json, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            throw new RuntimeException("JSON转List<String>失败: " + json, e);
        }
    }
}
