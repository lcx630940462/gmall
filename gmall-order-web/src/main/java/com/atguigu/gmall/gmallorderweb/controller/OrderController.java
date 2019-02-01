package com.atguigu.gmall.gmallorderweb.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.atguigu.gmall.bean.CartInfo;
import com.atguigu.gmall.bean.OrderDetail;
import com.atguigu.gmall.bean.OrderInfo;
import com.atguigu.gmall.bean.UserAddress;
import com.atguigu.gmall.config.LoginRequire;
import com.atguigu.gmall.enums.OrderStatus;
import com.atguigu.gmall.enums.ProcessStatus;
import com.atguigu.gmall.service.CartService;
import com.atguigu.gmall.service.OrderService;
import com.atguigu.gmall.service.UserInfoService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;

@Controller
public class OrderController {

   @Reference
    private UserInfoService userInfoService;

   @Reference
   private CartService cartService;

   @Reference
   private OrderService orderService;

    @RequestMapping("trade")
    @LoginRequire(autoRedirect = true)
    public String trade(HttpServletRequest request){
        String userId = (String) request.getAttribute("userId");
        //得到选中的购物车列表
        List<CartInfo> cartCheckedList = cartService.getCartCheckedList(userId);
        //地址
       List<UserAddress> userAddressList = userInfoService.findUserAddressByUserId(userId);
        request.setAttribute("userAddressList",userAddressList);

        //订单集合
        List<OrderDetail> orderDetailList=new ArrayList<>(cartCheckedList.size());
        for (CartInfo cartInfo : cartCheckedList) {
            OrderDetail orderDetail = new OrderDetail();
            orderDetail.setImgUrl(cartInfo.getImgUrl());
            orderDetail.setSkuId(cartInfo.getSkuId());
            orderDetail.setSkuName(cartInfo.getSkuName());
            orderDetail.setSkuNum(cartInfo.getSkuNum());
            orderDetail.setOrderPrice(cartInfo.getCartPrice());
            orderDetailList.add(orderDetail);
        }
        request.setAttribute("orderDetailList",orderDetailList);
        OrderInfo orderInfo = new OrderInfo();
        orderInfo.setOrderDetailList(orderDetailList);
        orderInfo.sumTotalAmount();
        request.setAttribute("totalAmount",orderInfo.getTotalAmount());
        // 获取TradeCode号
            String tradeNo = orderService.getTradeNo(userId);
        request.setAttribute("tradeNo",tradeNo);

        return "trade";
    }

    @RequestMapping("submitOrder")
    @LoginRequire
    public String submitOrder(HttpServletRequest request,OrderInfo orderInfo){
        String userId = (String) request.getAttribute("userId");
        //检查tradeNo
        String tradeNo = request.getParameter("tradeNo");
        boolean flag = orderService.checkTradeCode(userId, tradeNo);
        if (!flag){
            request.setAttribute("errMsg","页面已失效，请重新结算！");
            return "tradeFail";
        }

        //初始化参数
        orderInfo.setUserId(userId);
        //验库存
        List<OrderDetail> orderDetailList = orderInfo.getOrderDetailList();
        for (OrderDetail orderDetail : orderDetailList) {
            boolean result = orderService.checkStock(orderDetail.getSkuId(), orderDetail.getSkuNum());

            if (!result){//没有库存
                request.setAttribute("errMsg","商品库存不足，请重新下单！");
                return "tradeFail";
            }
        }


        //保存订单信息并获得orderId ，支付会根据订单id进行支付。
        String orderId = orderService.saveOrder(orderInfo);
        // 删除tradeNo
        orderService.delTradeCode(userId);

        //重定向
        return "redirect://payment.gmall.com/index?orderId="+orderId;
    }

}
