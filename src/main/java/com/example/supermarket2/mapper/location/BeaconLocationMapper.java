package com.example.supermarket2.mapper.location;

import com.example.supermarket2.entity.location.BeaconLocation;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 信标位置数据访问接口
 * MyBatis 映射，查询 beacon_location 表
 */
@Mapper
public interface BeaconLocationMapper {

    /**
     * 查询所有已启用的信标位置
     *
     * @return 已启用的信标位置列表
     */
    @Select("SELECT uuid, x, y FROM beacon_location WHERE enabled = 1")
    List<BeaconLocation> selectAllEnabled();
}
