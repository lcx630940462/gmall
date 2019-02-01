package com.atguigu.gmall.manage.mapper;

import tk.mybatis.mapper.common.Mapper;
import com.atguigu.gmall.bean.SkuSaleAttrValue;

import java.util.List;

public interface SkuSaleAttrValueMapper extends Mapper<SkuSaleAttrValue> {
    List<SkuSaleAttrValue> selectSkuSaleAttrValueListBySpu(String spuId);
}
