package com.example.supermarket2.mapper;


import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.supermarket2.entity.Product;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import java.util.List;

@Mapper
public interface ProductMapper extends BaseMapper<Product> {

    @Select("SELECT * FROM product WHERE status = 1 ORDER BY create_time DESC LIMIT #{limit}")
    List<Product> selectRecommendProducts(int limit);

    @Select("SELECT * FROM product WHERE status = 1 ORDER BY create_time DESC")
    List<Product> selectAllProducts();

    @Select("SELECT * FROM product WHERE status = 1 AND (name LIKE CONCAT('%', #{keyword}, '%') OR description LIKE CONCAT('%', #{keyword}, '%'))")
    List<Product> searchProducts(String keyword);

    @Select("SELECT * FROM product WHERE status = 1 AND id = #{id}")
    Product selectProductById(Long id);
}