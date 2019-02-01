package com.atguigu.gmall.service;

import com.atguigu.gmall.bean.OrderInfo;

public interface OrderService {
    public  String saveOrder(OrderInfo orderInfo);
    // 生成流水号
    public  String getTradeNo(String userId);
    // 验证流水号
    public  boolean checkTradeCode(String userId,String tradeCodeNo);
    // 删除流水号
    public void  delTradeCode(String userId);
    //验库存
   public boolean checkStock(String skuId, Integer skuNum);
    //根订单id查询订单详情，用于支付
    public  OrderInfo getOrderInfo(String orderId);
}
