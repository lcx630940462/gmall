package com.atguigu.gmall.passport.config;

import io.jsonwebtoken.*;

import java.util.Map;

public class JwtUtil {
    // encode 加密

    /**
     *
     * @param key 公共部分
     * @param param 私有部分
     * @param salt 签名
     * @return token
     */
    public static String encode(String key,Map<String,Object> param,String salt){
        if(salt!=null){
            key+=salt;
        }
        JwtBuilder jwtBuilder = Jwts.builder().signWith(SignatureAlgorithm.HS256,key);
        // 将用户的信息添加进去
        jwtBuilder = jwtBuilder.setClaims(param);

        String token = jwtBuilder.compact();
        return token;

    }

    // 解密

    /**
     *
     * @param token jwt 生成的字符串
     * @param key 公共部分
     * @param salt 签名部分
     * @return 返回私有部分
     */
    public  static Map<String,Object> decode(String token , String key, String salt){
        Claims claims=null;
        if (salt!=null){
            key+=salt;
        }
        try {
            claims= Jwts.parser().setSigningKey(key).parseClaimsJws(token).getBody();
        } catch ( JwtException e) {
            return null;
        }
        return  claims;
    }

}
