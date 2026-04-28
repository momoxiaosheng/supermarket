package com.example.supermarket2.mapper;


import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.supermarket2.entity.Product;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import java.util.List;

@Mapper
public interface ProductMapper extends BaseMapper<Product> {

    List<Product> selectRecommendProducts(int limit);

    List<Product> selectAllProducts();

    List<Product> searchProducts(String keyword);

    Product selectProductById(Long id);
}