package com.example.supermarket2.mapper;


import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.supermarket2.entity.Banner;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import java.util.List;

@Mapper
public interface BannerMapper extends BaseMapper<Banner> {

    @Select("SELECT * FROM banner WHERE status = 1 ORDER BY sort_order ASC")
    List<Banner> selectActiveBanners();
}
