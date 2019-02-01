package com.atguigu.gmall.cart.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.atguigu.gmall.bean.CartInfo;
import com.atguigu.gmall.bean.SkuInfo;
import com.atguigu.gmall.config.LoginRequire;
import com.atguigu.gmall.service.CartService;
import com.atguigu.gmall.service.ManageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;

@Controller
public class CartController {

    @Reference
    private CartService cartService;
    @Reference
    private ManageService manageService;

    @Autowired
    private CartCookieHandler cartCookieHandler;

    @RequestMapping("index")
    public  String index(){

        return "success";
    }

    /*
         HttpServletResponse response？？？？？？？？？？？
     */
    @RequestMapping("addToCart")
    @LoginRequire(autoRedirect = false) //必须加这个注解 ，拦截器中识别此注解，去做认证verify，认证成功，才能将userId存入request域中，方可在此处取值
    public String addToCart(HttpServletRequest request, HttpServletResponse response){
        //添加购物车需要从前台获得参数：skuId ，userId ， skuNum
        // 获取userId，skuId，skuNum
        String skuId = request.getParameter("skuId"); // 前台用value传递参数 th:value="${skuInfo.id}
        String skuNum = request.getParameter("skuNum");
        //从域中取出userId，之前在认证用户成功时，将userId存入request域中
        String userId = (String) request.getAttribute("userId");
        // 判断用户是否登录
        if (userId!=null){ //已登录
            cartService.addToCart(skuId,userId,Integer.parseInt(skuNum));

        }else {
            // 说明用户没有登录没有登录放到cookie中
            cartCookieHandler.addToCart(request,response,skuId,userId,Integer.parseInt(skuNum));

        }
        // 取得sku信息对象 ,用于success 页面显示添加购物车成功信息，显示商品图片 名称 个数 价格
        SkuInfo skuInfo = manageService.getSkuInfo(skuId);
        request.setAttribute("skuInfo",skuInfo);
        request.setAttribute("skuNum",skuNum);
        System.out.println((String) request.getAttribute("userId"));
        return "success";
    }

    /*
    Cookie: token=eyJhbGciOiJIUzI1NiJ9.eyJuaWNrTmFtZSI6IkFkbWluaXN0cmF0b3IiLCJ1c2VySWQiOiIyIn0.z9SlF6I3JNez0hIo8fZBvHMKap4bLmtc8YFRr1cKoNk; CART=%5B%7B%22cartPrice%22%3A5555%2C%22imgUrl%22%3A%22http%3A%2F%2F192.168.229.128%2Fgroup1%2FM00%2F00%2F00%2FwKjlgFv95MCAVTTfAABv-lCqbtw691.jpg%22%2C%22isChecked%22%3A%220%22%2C%22skuId%22%3A%2233%22%2C%22skuName%22%3A%22%E5%A4%A7%E7%B1%B320715+%E6%97%97%E8%88%B0%E7%89%88%22%2C%22skuNum%22%3A5%2C%22skuPrice%22%3A5555%7D%5D
     */

    @RequestMapping("cartList")
    @LoginRequire(autoRedirect = false)
    public String cartList(HttpServletRequest request,HttpServletResponse response){
        //判断用户是否登录，如果登录了，从redis取数据，如果redis没有数据，从数据库取
        //没有登录，直接从cookie中 取数据
        String  userId = (String) request.getAttribute("userId");
        if (userId!=null){
            //用户已登录, 合并购物车，如果cookie中有数据，则取出 与数据库 中的数据合并
            List<CartInfo> cartListFromCookie = cartCookieHandler.getCartList(request);
            List<CartInfo> cartList = null;
            if (cartListFromCookie!=null&&cartListFromCookie.size()>0){
                //需要合并
                cartList = cartService.mergeToCartList(cartListFromCookie,userId);
                // 删除cookie中的购物车
                cartCookieHandler.deleteCartCookie(request,response);

            }else {
                // 从redis中取得，或者从数据库中
                cartList = cartService.getCartList(userId);
            }
            request.setAttribute("cartList",cartList);
        }else {
            //用户未登录，从cookie取
            List<CartInfo> cartList = cartCookieHandler.getCartList(request);
            request.setAttribute("cartList",cartList);
        }

        return "cartList";
    }

    @RequestMapping("checkCart")
    @ResponseBody
    @LoginRequire(autoRedirect = false)
    public void  checkCart(HttpServletRequest request,HttpServletResponse response){
        String skuId = request.getParameter("skuId");
        String isChecked = request.getParameter("isChecked");
        String userId = (String) request.getAttribute("userId");

        if (userId!=null){
            //已登录
            cartService.checkCart(skuId,isChecked,userId);
        }else {
            //未登录，修改cookie中的信息
            cartCookieHandler.checkCart(request,response,skuId,isChecked);
        }

    }

    @RequestMapping("toTrade")
    @LoginRequire(autoRedirect = true)
    public String toTrade(HttpServletRequest request,HttpServletResponse response){
        String userId = (String) request.getAttribute("userId");
        List<CartInfo> cookieHandlerCartList = cartCookieHandler.getCartList(request);
        if (cookieHandlerCartList!=null &&cookieHandlerCartList.size()>0){
            cartService.mergeToCartList(cookieHandlerCartList, userId);
            cartCookieHandler.deleteCartCookie(request,response);
        }

        return "redirect://order.gmall.com/trade";
    }


}
