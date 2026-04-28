package com.example.supermarket2.handler;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;
import java.time.LocalDateTime;

@Component // 注册为Spring Bean
public class MyMetaObjectHandler implements MetaObjectHandler {

    // 插入时自动填充
    @Override
    public void insertFill(MetaObject metaObject) {
        // 填充createTime（如果字段为空）
        this.strictInsertFill(metaObject, "createTime", LocalDateTime.class, LocalDateTime.now());
        // 填充updateTime（如果字段为空）
        this.strictInsertFill(metaObject, "updateTime", LocalDateTime.class, LocalDateTime.now());
    }

    // 更新时自动填充
    @Override
    public void updateFill(MetaObject metaObject) {
        // 填充updateTime（更新时强制刷新）
        this.strictUpdateFill(metaObject, "updateTime", LocalDateTime.class, LocalDateTime.now());
    }
}
