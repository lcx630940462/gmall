package com.atguigu.gmall.manage.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.atguigu.gmall.bean.SkuInfo;
import com.atguigu.gmall.bean.SkuLsInfo;
import com.atguigu.gmall.bean.SpuImage;
import com.atguigu.gmall.bean.SpuSaleAttr;
import com.atguigu.gmall.service.ListService;
import com.atguigu.gmall.service.ManageService;
import org.apache.commons.beanutils.BeanUtils;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.lang.reflect.InvocationTargetException;
import java.util.List;
@Controller
public class SkuManageController {
    @Reference
    private ManageService manageService;

    @Reference
    private ListService listService;

    /*
    通过spuid查询spuImageList
 */
    @RequestMapping("spuImageList")
    @ResponseBody
    public List<SpuImage> spuImageList(String spuId){
        return manageService.getSpuImageList(spuId);
    }

    //根据spuId 查询销售属性spuSaleAttr 和销售属性值spu销售属性spuSaleAttrValue
    @RequestMapping("spuSaleAttrList")
    @ResponseBody
    public List<SpuSaleAttr> spuSaleAttrList(String spuId){
        return manageService.getSpuSaleAttrList(spuId);
    }

    @RequestMapping("saveSku")
    @ResponseBody
    public String saveSkuInfo(SkuInfo skuInfo){
        manageService.saveSkuInfo(skuInfo);
        return "success";

    }

    //商品上架，根据商品进行上架
    @RequestMapping("onSale")
    @ResponseBody
    public void onSale(String skuId){
        // 根据skuId 查询skuInfo
        SkuInfo skuInfo = manageService.getSkuInfo(skuId);

        // skuLsInfo 所有数据来源于skuInfo ，必须先查询到skuInfo
        SkuLsInfo skuLsInfo = new SkuLsInfo();
        // 开始给skuLsInfo赋值
        try {
            BeanUtils.copyProperties(skuLsInfo,skuInfo);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
        // org.springframework.beans.BeanUtils.copyProperties(skuInfo,skuLsInfo);
        // 调用 方法
        listService.saveSkuInfo(skuLsInfo);
    }


}
