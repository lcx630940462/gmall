package com.atguigu.gmall.manage.service;

import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.bean.*;
import com.atguigu.gmall.config.RedisUtil;
import com.atguigu.gmall.manage.constant.ManageConst;
import com.atguigu.gmall.manage.mapper.*;
import com.atguigu.gmall.service.ManageService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import redis.clients.jedis.Jedis;

import java.util.List;


@Service
public class ManageServiceImpl implements ManageService {

    @Autowired
    private BaseCatalog1Mapper baseCatalog1Mapper;

    @Autowired
    private BaseCatalog2Mapper baseCatalog2Mapper;

    @Autowired
    private BaseCatalog3Mapper baseCatalog3Mapper;

    @Autowired
    private BaseAttrInfoMapper baseAttrInfoMapper;

    @Autowired
    private BaseAttrValueMapper baseAttrValueMapper;

    @Autowired
    private SpuInfoMapper spuInfoMapper;

    @Autowired
    private BaseSaleAttrMapper baseSaleAttrMapper;

    @Autowired
    private  SpuImageMapper spuImageMapper;

    @Autowired
    private  SpuSaleAttrMapper spuSaleAttrMapper;

    @Autowired
    private  SpuSaleAttrValueMapper spuSaleAttrValueMapper;

    @Autowired
    private  SkuInfoMapper skuInfoMapper;

    @Autowired
    private SkuImageMapper skuImageMapper;

    @Autowired
    private SkuAttrValueMapper skuAttrValueMapper;

    @Autowired
    private SkuSaleAttrValueMapper skuSaleAttrValueMapper;

    @Autowired
    private RedisUtil redisUtil;



    @Override
    public List<BaseCatalog1> getCatalog1() {
        return baseCatalog1Mapper.selectAll();
    }

    @Override
    public List<BaseCatalog2> getCatalog2(String catalog1Id) {

        BaseCatalog2 baseCatalog2 = new BaseCatalog2();
        baseCatalog2.setCatalog1Id(catalog1Id);
        List<BaseCatalog2> baseCatalog2List = baseCatalog2Mapper.select(baseCatalog2);

        return baseCatalog2List;
    }

    @Override
    public List<BaseCatalog3> getCatalog3(String catalog2Id) {
        BaseCatalog3 baseCatalog3 = new BaseCatalog3();
        baseCatalog3.setCatalog2Id(catalog2Id);
        List<BaseCatalog3> baseCatalog3List = baseCatalog3Mapper.select(baseCatalog3);
        return baseCatalog3List;
    }

   @Override
    public List<BaseAttrInfo> getAttrList(List<String> attrValueIdList) {
        // 数据来源 baseAttrInfo  ，baseAttrValue  条件是 baseAttrValue.id
        // mybatis
        /*
             select * FROM base_attr_info bai INNER JOIN base_attr_value bav ON
            bai.id = bav.attr_id WHERE bav.id in
            <foreach item="valueId" open="(" close=")" separator="," collection="list" >
            #{valueId}
         */
        // select * FROM base_attr_info bai INNER JOIN base_attr_value bav ON
        // bai.id = bav.attr_id WHERE bav.id in(82,14)
        String attrValueIds  = StringUtils.join(attrValueIdList.toArray(), ",");
        // attrValueIds  {82,14,,...}
        List<BaseAttrInfo>  baseAttrInfoList = baseAttrInfoMapper.selectAttrInfoListByIds(attrValueIds);
        // 保存平台属性集合
        return baseAttrInfoList;

    }

    @Override
    public List<BaseAttrInfo> getAttrList(String catalog3Id) {
        // select * from baseAttrInfo where catalog3Id=?
//        BaseAttrInfo baseAttrInfo = new BaseAttrInfo();
//        baseAttrInfo.setCatalog3Id(catalog3Id);
//        return baseAttrInfoMapper.select(baseAttrInfo);
        return  baseAttrInfoMapper.getBaseAttrInfoListByCatlog3Id(Long.parseLong(catalog3Id));
    }

    @Override
    public void saveAttrInfo(BaseAttrInfo baseAttrInfo) {
        //要保存的数据 原来没有
        if (baseAttrInfo.getId()==null||baseAttrInfo.getId().length()==0){
            if (baseAttrInfo.getId().length()==0){
                baseAttrInfo.setId(null);
            }
            baseAttrInfoMapper.insertSelective(baseAttrInfo);
        }else {
            baseAttrInfoMapper.updateByPrimaryKeySelective(baseAttrInfo);
        }

        //要保持的数据已经在数据库存在 修改原来数据 ，先删除 再添加
        BaseAttrValue baseAttrValue = new BaseAttrValue();
        baseAttrValue.setAttrId(baseAttrInfo.getId());
        baseAttrValueMapper.delete(baseAttrValue);

        //插入数据
        List<BaseAttrValue> attrValueList = baseAttrInfo.getAttrValueList();
        if (attrValueList!=null && attrValueList.size()>0){
            for (BaseAttrValue attrValue : attrValueList) {
                if (attrValue.getId().length()==0){
                    attrValue.setId(null);
                }
                //添加attrid的值
                attrValue.setAttrId(baseAttrInfo.getId());
                baseAttrValueMapper.insertSelective(attrValue);
            }
        }
    }

    @Override
    public List<SpuInfo> getSpuInfoList(SpuInfo spuInfo) {

        List<SpuInfo> spuInfoList = spuInfoMapper.select(spuInfo);
        return  spuInfoList;
    }

    @Override
    public List<BaseSaleAttr> getBaseSaleAttrList() {
        return baseSaleAttrMapper.selectAll();
    }

    @Override
    public void saveSpuInfo(SpuInfo spuInfo) {
        // 判断spuInfo.Id 是否为空 null or "" spuInfo
        if (spuInfo.getId()==null || spuInfo.getId().length()==0){
            // 意味着添加数据
            if (spuInfo.getId().length()==0){
                spuInfo.setId(null);
            }
            spuInfoMapper.insertSelective(spuInfo);
        }else {
            // 更新数据
            spuInfoMapper.updateByPrimaryKeySelective(spuInfo);
        }
        // 先将spuImage 中相关与当前spuInfo.Id 的数据进行删除
        SpuImage spuImageDel = new SpuImage();
        spuImageDel.setSpuId(spuInfo.getId());
        spuImageMapper.delete(spuImageDel);

        // spuImage ,从前台获取数据，
        List<SpuImage> spuImageList = spuInfo.getSpuImageList();
        if (spuImageList!=null && spuImageList.size()>0){
            // 循环添加数据
            for (SpuImage spuImage : spuImageList) {
                // 设计到主键自增
                // 面试官问你，如何解决空指针异常！if(xxx!=null) "" ,null
                spuImage.setId(null);
                // 给spuId 赋值
                spuImage.setSpuId(spuInfo.getId());
                // 数据插入
                spuImageMapper.insertSelective(spuImage);
            }
        }
        // spuSaleAttr 销售属性 先删除
        // spuSaleAttrValue 销售属性值  先删除
        SpuSaleAttr spuSaleAttr = new SpuSaleAttr();
        spuSaleAttr.setSpuId(spuInfo.getId());
        spuSaleAttrMapper.delete(spuSaleAttr);

        SpuSaleAttrValue spuSaleAttrValue = new SpuSaleAttrValue();
        spuSaleAttrValue.setSpuId(spuInfo.getId());
        spuSaleAttrValueMapper.delete(spuSaleAttrValue);

        // 插入数据，先从前台获取到数据 所有的销售属性集合！
        List<SpuSaleAttr> spuSaleAttrList = spuInfo.getSpuSaleAttrList();
        if (spuSaleAttrList!=null && spuSaleAttrList.size()>0){
            for (SpuSaleAttr saleAttr : spuSaleAttrList) {
                saleAttr.setId(null);
                // 添加spuid
                saleAttr.setSpuId(spuInfo.getId());
                // 保存
                spuSaleAttrMapper.insertSelective(saleAttr);
                // 销售属性值在哪？在SpuSaleAttr对象中取得spuSaleAttrValueList
                List<SpuSaleAttrValue> spuSaleAttrValueList = saleAttr.getSpuSaleAttrValueList();
                if (spuSaleAttrValueList!=null && spuSaleAttrValueList.size()>0){
                    // 循环添加
                    for (SpuSaleAttrValue saleAttrValue : spuSaleAttrValueList) {
                        saleAttrValue.setId(null);
                        // 添加spuId
                        saleAttrValue.setSpuId(spuInfo.getId());
                        // 插入数据
                        spuSaleAttrValueMapper.insertSelective(saleAttrValue);
                    }
                }
            }
        }
    }

    @Override
    public List<SpuImage> getSpuImageList(String spuId) {
        SpuImage spuImage = new SpuImage();
        spuImage.setSpuId(spuId);
        return spuImageMapper.select(spuImage);
    }

    @Override
    public void deleteAttrInfoByPremaryKey(String id) {
        baseAttrInfoMapper.deleteByPrimaryKey(id);
    }

    @Override
    public List<SpuSaleAttr> getSpuSaleAttrList(String spuId) {
        return spuSaleAttrMapper.selectSpuAttrListBySpuId(spuId);
    }

    @Override
    public void saveSkuInfo(SkuInfo skuInfo) {
        if(skuInfo.getId()==null||skuInfo.getId().length()==0){
            skuInfo.setId(null);
            skuInfoMapper.insertSelective(skuInfo);

        }else {
            skuInfoMapper.updateByPrimaryKeySelective(skuInfo);
        }

        //sku_img
        SkuImage skuImage = new SkuImage();
        skuImage.setSkuId(skuInfo.getId());
        skuImageMapper.delete(skuImage);

        //insert
        List<SkuImage> skuImageList = skuInfo.getSkuImageList();
        if (skuImageList!=null&&skuImageList.size()>0){
            for (SkuImage image : skuImageList) {
                if (image.getId()!=null&&image.getId().length()==0){
                    image.setId(null);
                }
                image.setSkuId(skuInfo.getId());
                skuImageMapper.insertSelective(image);
            }
        }

        //   sku_attr_value, 先delete
        SkuAttrValue skuAttrValue = new SkuAttrValue();
        skuAttrValue.setSkuId(skuInfo.getId());
        skuAttrValueMapper.delete(skuAttrValue);

        // 插入数据
        List<SkuAttrValue> skuAttrValueList = skuInfo.getSkuAttrValueList();
        if (skuAttrValueList!=null &&skuAttrValueList.size()>0) {
            for (SkuAttrValue attrValue : skuAttrValueList) {
                if (attrValue.getId() != null && attrValue.getId().length() == 0) {
                    attrValue.setId(null);
                }
                attrValue.setSkuId(skuInfo.getId());
                skuAttrValueMapper.insertSelective(attrValue);
            }
        }

        //        sku_sale_attr_value, delete
        SkuSaleAttrValue skuSaleAttrValue = new SkuSaleAttrValue();
        skuSaleAttrValue.setSkuId(skuInfo.getId());
        skuSaleAttrValueMapper.delete(skuSaleAttrValue);

        //      插入数据
        List<SkuSaleAttrValue>skuSaleAttrValueList = skuInfo.getSkuSaleAttrValueList();
        if (skuSaleAttrValueList!=null &&skuSaleAttrValueList.size()>0){
            for (SkuSaleAttrValue saleAttrValue : skuSaleAttrValueList) {
                if (saleAttrValue.getId()!=null &&saleAttrValue.getId().length()==0){
                    saleAttrValue.setId(null);
                }
                // skuId
                saleAttrValue.setSkuId(skuInfo.getId());
                skuSaleAttrValueMapper.insertSelective(saleAttrValue);
            }
        }

    }


    @Override
    public SkuInfo getSkuInfo(String skuId) {

        SkuInfo skuInfo = null;
        try {
            // 获取redis
            Jedis jedis = redisUtil.getJedis();
            // 定义key sku:skuId:info
            String skuInfoKey = ManageConst.SKUKEY_PREFIX+skuId+ManageConst.SKUKEY_SUFFIX; //key= sku:skuId:info
            // 取数据
            String skuJson  = jedis.get(skuInfoKey);
            if (skuJson==null || "".equals(skuJson)){
                // 没有数据
                System.out.println("缓存没有数据，上锁");
                // 定义一个锁的key
                String skuLockKey = ManageConst.SKUKEY_PREFIX + skuId + ManageConst.SKULOCK_SUFFIX;
                // 运行命令
                String lockKey   = jedis.set(skuLockKey, "OK", "NX", "PX", ManageConst.SKULOCK_EXPIRE_PX);
                if ("OK".equals(lockKey)){
                    System.out.println("上锁，准备从mysql 取得数据");
                    skuInfo = getSkuInfoDB(skuId);
                    // 将对象转换为字符串
                    String skuJsons = JSON.toJSONString(skuInfo);
                    // 将数据放入redis
                    // jedis.set(skuKey, skuJsons);
                    jedis.setex(skuInfoKey,ManageConst.SKUKEY_TIMEOUT,skuJsons);
                    jedis.close();
                    return  skuInfo;
                }else {
                    // 等待 ，睡一会！
                    Thread.sleep(1000);
                    // 然后继续查询
                    getSkuInfo(skuId);
                }
            }else {
                // 取得数据，从redis中取得
                skuInfo  = JSON.parseObject(skuJson, SkuInfo.class);

                return  skuInfo;
            }
        } catch (Exception e){
            e.printStackTrace();
        }
        return  getSkuInfoDB(skuId);
    }

    public SkuInfo getSkuInfoDB(String skuId){
        // ctrl+alt+m
        // skuId = skuInfo.id
        SkuInfo skuInfo = skuInfoMapper.selectByPrimaryKey(skuId);
        // 根据skuId 查询skuImageList
        SkuImage skuImage = new SkuImage();
        skuImage.setSkuId(skuId);
        List<SkuImage> skuImageList = skuImageMapper.select(skuImage);
        skuInfo.setSkuImageList(skuImageList);
        //sku平台属性集合
        SkuAttrValue skuAttrValue = new SkuAttrValue();
        skuAttrValue.setSkuId(skuId);
        List<SkuAttrValue> skuAttrValueList = skuAttrValueMapper.select(skuAttrValue);
        skuInfo.setSkuAttrValueList(skuAttrValueList);

        return skuInfo;
    }

    @Override
    public List<SpuSaleAttr> selectSpuSaleAttrListCheckBySku(SkuInfo skuInfo) {
        return spuSaleAttrMapper.selectSpuSaleAttrListCheckBySku(Long.parseLong(skuInfo.getId()),Long.parseLong(skuInfo.getSpuId()));
    }

    @Override
    public List<SkuSaleAttrValue> getSkuSaleAttrValueListBySpu(String spuId) {
        return skuSaleAttrValueMapper.selectSkuSaleAttrValueListBySpu(spuId);
    }

}
