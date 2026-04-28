package com.example.supermarket2.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.supermarket2.entity.Category;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import java.util.List;

@Mapper
public interface CategoryMapper extends BaseMapper<Category> {

    List<Category> selectRootCategories();
}