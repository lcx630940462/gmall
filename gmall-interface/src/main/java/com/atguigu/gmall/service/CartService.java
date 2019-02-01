package com.atguigu.gmall.service;

import com.atguigu.gmall.bean.CartInfo;

import java.util.List;

public interface CartService {
    //添加到购物车
    public  void addToCart(String skuId,String userId,Integer skuNum);
    // 查询购物车集合列表
    public List<CartInfo> getCartList(String userId);

    //合并购物车，cookie与数据库中的数据合并
    public  List<CartInfo> mergeToCartList(List<CartInfo> cartListFromCookie, String userId);
    //购物车，勾选框变化，改变redis中的购物车缓存信息，数据库中没有isChecked字段
    public void checkCart(String skuId, String isChecked, String userId);
    //从redis中查询被选中购物车信息集合
    public List<CartInfo> getCartCheckedList(String userId);
}
