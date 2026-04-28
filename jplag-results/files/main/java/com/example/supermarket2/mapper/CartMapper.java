package com.example.supermarket2.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.supermarket2.entity.Cart;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import java.util.List;

@Mapper
public interface CartMapper extends BaseMapper<Cart> {

    @Select("SELECT * FROM cart WHERE user_id = #{userId}")
    List<Cart> selectByUserId(Long userId);

    @Select("SELECT COUNT(*) FROM cart WHERE user_id = #{userId}")
    Integer countByUserId(Long userId);
}
