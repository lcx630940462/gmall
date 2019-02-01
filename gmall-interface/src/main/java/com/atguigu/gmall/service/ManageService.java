package com.atguigu.gmall.service;

import com.atguigu.gmall.bean.*;

import java.util.List;

public interface ManageService {
    public List<BaseCatalog1> getCatalog1();

    public List<BaseCatalog2> getCatalog2(String catalog1Id);

    public List<BaseCatalog3> getCatalog3(String catalog2Id);

    public List<BaseAttrInfo>getAttrList(List<String> attrValueIdList);

    public List<BaseAttrInfo> getAttrList(String catalog3Id);

    public  void saveAttrInfo(BaseAttrInfo baseAttrInfo);

    public List<SpuInfo> getSpuInfoList(SpuInfo spuInfo);

    // 查询基本销售属性表
    List<BaseSaleAttr> getBaseSaleAttrList();

    //保存spu
    void saveSpuInfo(SpuInfo spuInfo);

    //根据spuid 查询spuimg
    List<SpuImage> getSpuImageList(String spuId);

    //根据平台属性id 删除平台数据
    void deleteAttrInfoByPremaryKey(String id);

    //根据spuId 查询销售属性spuSaleAttr 和销售属性值spu销售属性spuSaleAttrValue
    List<SpuSaleAttr> getSpuSaleAttrList(String spuId);

    // 保存skuInfo
    void saveSkuInfo(SkuInfo skuInfo);

    SkuInfo getSkuInfo(String skuId);
    
    List<SpuSaleAttr> selectSpuSaleAttrListCheckBySku(SkuInfo skuInfo);

    List<SkuSaleAttrValue> getSkuSaleAttrValueListBySpu(String spuId);
}
