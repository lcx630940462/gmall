package com.atguigu.gmall.passport.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.atguigu.gmall.bean.UserInfo;
import com.atguigu.gmall.passport.config.JwtUtil;

import com.atguigu.gmall.service.UserInfoService;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;


import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

@Controller
public class PassportController {

 @RequestMapping("index")
    public String index (HttpServletRequest request){
        String originUrl = request.getParameter("originUrl");

        request.setAttribute("originUrl",originUrl);
        return "index";
    }
    /*
         String key = "atguigu"; 在配置文件设置 token.key
                                 @Value("${token.key}")
                                 private String signKey;
         String salt="192.168.229.1";  服务器的ip 作为盐
         在nginx.conf中配置：
             upstream passport.atguigu.com{
                   server 192.168.229.1:8087;
                }
               server {
                 listen 80;
            server_name passport.atguigu.com;
                 location / {
            proxy_pass http://passport.atguigu.com;
            proxy_set_header X-forwarded-for $proxy_add_x_forwarded_for;
                 }
                }
        token:eyJhbGciOiJIUzI1NiJ9.eyJuaWNrTmFtZSI6Im1hcnJ5IiwidXNlcklkIjoiMTAwMSJ9.6rEpYWiZC1VSDe1z9yR2nfsvNobvBs8SRJjjoIx7zvk
        {nickName=marry, userId=1001}

     */

    @Reference
    private UserInfoService userInfoService;

    @Value("${token.key}")
    private String signKey; //key

    @RequestMapping("login")
    @ResponseBody
    public String login (UserInfo userInfo,HttpServletRequest request){

       String salt = request.getHeader("X-forwarded-for");//盐
        if (userInfo!=null){
            //String salt="192.168.229.1";
            UserInfo info = userInfoService.login(userInfo);//通过用户输入的信息查询数据库 有的话返回userInfo，用户不存在返回null
            System.out.println("info:"+info);
            if (info!=null){
                Map<String,Object> map = new HashMap();//map 作用？？？？
                map.put("userId",info.getId());
                map.put("nickName",info.getNickName());
                String token = JwtUtil.encode(signKey, map, salt);
                System.out.println("newToken:"+token);
                return token;
            }else {
                return "fail";
            }
        }else {
            return "fail";
        }

    }

    //用户认证
    @RequestMapping("verify")
    @ResponseBody
    public String verify(HttpServletRequest request){
        //解密：需要key token salt
       String token = request.getParameter("token");
       String salt = request.getParameter("currentIp");
       Map<String,Object> map = JwtUtil.decode(token,signKey,salt);

       if (map!=null){
           //与redis 做查询，
           String userId = (String) map.get("userId");
           UserInfo userInfo = userInfoService.verify(userId);
            if (userInfo!=null){
                return "success";
            }else {
                return "fail";
            }

       }return "fail";

    }
    /*  测试url
        eyJhbGciOiJIUzI1NiJ9.eyJuaWNrTmFtZSI6IkFkbWluaXN0cmF0b3IiLCJ1c2VySWQiOiIyIn0.z9SlF6I3JNez0hIo8fZBvHMKap4bLmtc8YFRr1cKoNk
        http://passport.atguigu.com/verify?token=eyJhbGciOiJIUzI1NiJ9.eyJuaWNrTmFtZSI6IkFkbWluaXN0cmF0b3IiLCJ1c2VySWQiOiIyIn0.z9SlF6I3JNez0hIo8fZBvHMKap4bLmtc8YFRr1cKoNk&currentIp=192.168.229.1

     */


}
