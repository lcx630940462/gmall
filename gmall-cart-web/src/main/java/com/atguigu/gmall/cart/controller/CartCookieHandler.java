package com.atguigu.gmall.cart.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.fastjson.JSON;

import com.atguigu.gmall.bean.CartInfo;
import com.atguigu.gmall.bean.SkuInfo;
import com.atguigu.gmall.config.CookieUtil;
import com.atguigu.gmall.service.ManageService;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.List;

/**
 * 用来操作所有cookie 中的数据
 */
@Component
public class CartCookieHandler {
    // 定义购物车名称
    private String COOKIECARTNAME = "CART";
    // 设置cookie 过期时间
    private int COOKIE_CART_MAXAGE=7*24*3600;
    
    @Reference
    private ManageService manageService;

    /**
     * 未登录时，添加购物车cookie
     * @param request
     * @param response
     * @param skuId
     * @param userId
     * @param skuNum
     */
    public void addToCart(HttpServletRequest request, HttpServletResponse response, String skuId, String userId, int skuNum) {
        // 获取cookie 中的所有数据
        String cartJson = CookieUtil.getCookieValue(request, COOKIECARTNAME, true);

        // 声明一个空的集合来存储cookie 中购物车的所有数据
        List<CartInfo> cartInfoList = new ArrayList<>();
        // cartJson 表示购物车集合 ，进行转换集合形式

        // 循环比较该购物车中是否有该商品
        // 借助一个boolean 类型的变量
        boolean ifExist=false;
        if (cartJson!=null && cartJson.length()>0){
            cartInfoList = JSON.parseArray(cartJson, CartInfo.class);
            for (CartInfo cartInfo : cartInfoList) {
                // 能够匹配上
                if (cartInfo.getSkuId().equals(skuId)){
                    cartInfo.setSkuNum(cartInfo.getSkuNum()+skuNum);
                    // cartInfoList 放入购物车！
                    ifExist = true;
                    break;
                }
            }
        }
        // 添加的商品在购物车中不存在！
        if (!ifExist){
            // 直接添加
            SkuInfo skuInfo = manageService.getSkuInfo(skuId);
            // 进行属性拷贝！
            CartInfo cartInfo=new CartInfo();
            cartInfo.setSkuId(skuId);
            cartInfo.setCartPrice(skuInfo.getPrice());
            cartInfo.setSkuPrice(skuInfo.getPrice());
            cartInfo.setSkuName(skuInfo.getSkuName());
            cartInfo.setImgUrl(skuInfo.getSkuDefaultImg());

            cartInfo.setUserId(userId);
            cartInfo.setSkuNum(skuNum);

            // 购物车中没有的商品添加到购物车中！
            cartInfoList.add(cartInfo);
        }
        // 集合转换为字符串
        String newCartJson  = JSON.toJSONString(cartInfoList);
        // 将购物车集合放入cookie 中！
        CookieUtil.setCookie(request,response,COOKIECARTNAME,newCartJson,COOKIE_CART_MAXAGE,true);
    }
    // 取得购物车数据
    public List<CartInfo> getCartList(HttpServletRequest request) {
        // 取得购物车数据
        String cartJson = CookieUtil.getCookieValue(request, COOKIECARTNAME, true);
        // 进行转换
        List<CartInfo> cartInfoList = JSON.parseArray(cartJson, CartInfo.class);
        // 返回集合对象
        return cartInfoList;
    }

    // 删除cookie 中的数据
    public void deleteCartCookie(HttpServletRequest request, HttpServletResponse response) {
            CookieUtil.deleteCookie(request,response,COOKIECARTNAME);

    }

    //更新cookie中的购物车信息
    public void checkCart(HttpServletRequest request, HttpServletResponse response, String skuId, String isChecked) {
        //取出购物车中的信息
        List<CartInfo> cartList = getCartList(request);
        //循环比较
        for (CartInfo cartInfo : cartList) {
           if (  skuId.equals(cartInfo.getSkuId()) ){
               cartInfo.setIsChecked(isChecked);
           }
        }

        String newCartJson  = JSON.toJSONString(cartList);
        //保存cookie
        CookieUtil.setCookie(request,response,COOKIECARTNAME,newCartJson,COOKIE_CART_MAXAGE,true);


    }
}
