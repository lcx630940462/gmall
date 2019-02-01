package com.atguigu.gmall.manage.mapper;

import com.atguigu.gmall.bean.BaseAttrInfo;
import org.apache.ibatis.annotations.Param;
import tk.mybatis.mapper.common.Mapper;

import java.util.List;

public interface BaseAttrInfoMapper extends Mapper<BaseAttrInfo> {

    //根据三级分类id查找属性表
   List<BaseAttrInfo> getBaseAttrInfoListByCatlog3Id(Long catlog3Id);
    // 根据传入进来的平台数值Id进行查询 82,14
    List<BaseAttrInfo> selectAttrInfoListByIds(@Param("valueIds") String valueIds);
}

