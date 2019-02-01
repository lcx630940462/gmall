package com.atguigu.gmall.config;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.util.HttpClientUtil;
import io.jsonwebtoken.impl.Base64UrlCodec;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Map;

//拦截器 1。继承  2.实现
@Component
public class AuthInterceptor extends HandlerInterceptorAdapter {
    //进入控制器之前执行操作
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        //将token放入cookie
        String token = request.getParameter("newToken");
        if (token!=null){
            CookieUtil.setCookie(request,response,"token",token,WebConst.COOKIE_MAXAGE,false);
            System.out.println("putIntoCookie**************");
        }
        //用户登录之后，访问其他页面项目，request中取不到值，token为null，从cookie中取出token
        if (token==null){
            token = CookieUtil.getCookieValue(request, "token", false);
            System.out.println("访问其他页面************");
        }
        //从cookie取出的token不为null ，说明用户登录了，将昵称放入request域中
        if(token!=null) {
            //读取token
            Map map = getUserMapByToken(token);//解密token
            String nickName = (String) map.get("nickName");
            request.setAttribute("nickName", nickName);
            System.out.println("nickname****************:"+nickName);
        }
        //在拦截器中获取获取方法上的自定义注解，handler可以获得 当前类上的所有注解 和类名
        HandlerMethod handlerMethod = (HandlerMethod) handler;
        //获取方法上的注解
        LoginRequire methodAnnotation = handlerMethod.getMethodAnnotation(LoginRequire.class);
        if (methodAnnotation!=null){
            //有注解，需要去验证登录 verify,做认证， 调用verify 需要参数 token ，salt（ip）
            String remoteAddr = request.getHeader("x-forwarded-for");
            String result = HttpClientUtil.doGet(WebConst.VERIFY_ADDRESS + "?token=" + token + "&currentIp=" + remoteAddr);
            if ("success".equals(result)){
                //认证成功，解密token，保存用户id
                Map map = getUserMapByToken(token);
                String userId =(String) map.get("userId");
                request.setAttribute("userId",userId);
                return true;
            }else{
                //认证失败，需要登录则跳转到登录页面
                if(methodAnnotation.autoRedirect()){
                    //autoRedirect 默认true 必须登录， 获得当前url
                    String  requestURL = request.getRequestURL().toString();
                    String encodeURL = URLEncoder.encode(requestURL, "UTF-8");
                    //将页面信息转发重定向到上一个页面
                    response.sendRedirect(WebConst.LOGIN_ADDRESS+"?originUrl="+encodeURL);
                    return false;
                }
            }

        }


        return true;
    }
    //解密token，得到token中的私有部分
    private Map getUserMapByToken(String token) {
        String tokenUserInfo = StringUtils.substringBetween(token, ".");//取得token中间部分，即私有信息 userInfo
        Base64UrlCodec base64UrlCodec = new Base64UrlCodec();//对私有部分进行base64解码
        byte[] tokenBytes = base64UrlCodec.decode(tokenUserInfo);
        String tokenJson = null;
        try {
            tokenJson = new String(tokenBytes, "UTF-8");    //将字节数组转换为json字符串
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        Map map = JSON.parseObject(tokenJson, Map.class); //字符串转换为map
        return map;

    }

}
