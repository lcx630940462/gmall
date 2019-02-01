package com.atguigu.gmall.cart.service.impl;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.bean.CartInfo;
import com.atguigu.gmall.bean.SkuInfo;
import com.atguigu.gmall.cart.constant.CartConst;
import com.atguigu.gmall.cart.mapper.CartInfoMapper;
import com.atguigu.gmall.config.RedisUtil;
import com.atguigu.gmall.service.CartService;
import com.atguigu.gmall.service.ManageService;
import org.springframework.beans.factory.annotation.Autowired;
import redis.clients.jedis.Jedis;

import java.util.*;

@Service
public class CartServiceImpl implements CartService{
    @Autowired
    private CartInfoMapper cartInfoMapper;

    @Reference
    private ManageService manageService;

    @Autowired
    private RedisUtil redisUtil;

    //添加购物车
    @Override
    public void addToCart(String skuId, String userId, Integer skuNum) {
        //1.先查购物车是否有该商品sku
        CartInfo cartInfo = new CartInfo();
        cartInfo.setSkuId(skuId);
        CartInfo cartInfoExist  = cartInfoMapper.selectOne(cartInfo);
        if (cartInfoExist!=null){//购物车已有该sku，更新
            //更新数量
            cartInfoExist.setSkuNum(cartInfoExist.getSkuNum()+skuNum);
            //更新实时价格
            cartInfoExist.setSkuPrice(cartInfoExist.getCartPrice());
            //更新数据库cartInfo
            cartInfoMapper.updateByPrimaryKeySelective(cartInfoExist);
        }else {
            //购物车没有该商品 ，添加
             SkuInfo skuInfo = manageService.getSkuInfo(skuId);
            CartInfo cartInfo1 = new CartInfo();
                cartInfo1.setUserId(userId);
                cartInfo1.setSkuId(skuId);
                cartInfo1.setSkuNum(skuNum);
                cartInfo1.setSkuPrice(skuInfo.getPrice());
                cartInfo1.setCartPrice(skuInfo.getPrice());
                cartInfo1.setImgUrl(skuInfo.getSkuDefaultImg());
                cartInfo1.setSkuName(skuInfo.getSkuName());
            //插入数据库
            cartInfoMapper.insert(cartInfo1);
            cartInfoExist = cartInfo1;
        }
        //不管购物车是否有该商品，都要更新redis缓存数据库
        /* redis 用hash存储
             key：“user:[userId]:cart”
             field:     [skuId]
             value:    CartInfo  (Json)
         */
        //构建 key user:userid:cart
        String userCartKey = CartConst.USER_KEY_PREFIX+userId+CartConst.USER_CART_KEY_SUFFIX;
        //准备取数据
        Jedis jedis = redisUtil.getJedis();
        //将cartInfoExist对象 序列化为json
        String cartJson  = JSON.toJSONString(cartInfoExist);
        //存入redis
        jedis.hset(userCartKey,skuId,cartJson);
        //更新购物车过期时间 与用户在redis的过期时间一致，用户没有登录 自然不需要存储该用户的购物车
       String userInfoKey = CartConst.USER_KEY_PREFIX+userId+CartConst.USERINFOKEY_SUFFIX;
        Long ttl = jedis.ttl(userInfoKey);
        jedis.expire(userCartKey,ttl.intValue());
        jedis.close();

    }


    @Override
    public List<CartInfo> getCartList(String userId) {
        //从redis中取数据
        Jedis jedis = redisUtil.getJedis();
        String userCartKey = CartConst.USER_KEY_PREFIX+userId+CartConst.USER_CART_KEY_SUFFIX;
        List<String> cartJsons = jedis.hvals(userCartKey);

        if (cartJsons!=null&&cartJsons.size()>0){
            //redis中有数据, 将redis中的数据存入 cartInfoList
            List<CartInfo> cartInfoList = new ArrayList<>();
            for (String cartJson : cartJsons) {
                CartInfo cartInfo = JSON.parseObject(cartJson, CartInfo.class);
                cartInfoList.add(cartInfo);
            }
            // 排序
            cartInfoList.sort(new Comparator<CartInfo>() {
                @Override
                public int compare(CartInfo o1, CartInfo o2) {
                    return Long.compare(Long.parseLong(o2.getId()),Long.parseLong(o1.getId()));
                }
            });
            jedis.close();
            return cartInfoList;
        }else{
            // 从数据库中查询，其中cart_price 可能是旧值，所以需要关联sku_info 表信息。
            List<CartInfo> cartInfoList = loadCartCache(userId);
            return  cartInfoList;
        }

    }

    @Override
    public List<CartInfo> mergeToCartList(List<CartInfo> cartListFromCookie, String userId) {
        //数据库中的购物车信息
        List<CartInfo> cartInfoListDB = cartInfoMapper.selectCartListWithCurPrice(userId);
        //循环匹配，有相同的sku ，数量增加 ，没有则创建新的数据添加入数据库
        for (CartInfo cartInfoCK : cartListFromCookie) {
            boolean isMatch = false;
            for (CartInfo cartInfoDB : cartInfoListDB) {
                if (cartInfoDB.getSkuId().equals(cartInfoCK.getSkuId())) {
                    cartInfoDB.setSkuNum(cartInfoDB.getSkuNum() + cartInfoCK.getSkuNum());//数量相加
                    cartInfoMapper.updateByPrimaryKeySelective(cartInfoDB);
                    isMatch = true;
                }
            }
            // 数据库中没有购物车，则直接将cookie中购物车直接添加到数据库
            if (!isMatch) {
                cartInfoCK.setUserId(userId); //必须先设置userId，cookie中不包含userId信息
                cartInfoMapper.insertSelective(cartInfoCK);
            }

        }
        // 购物车查询，在数据库中查找，并更新redis
        List<CartInfo> cartInfoList = loadCartCache(userId);
        // 用户未登录状态下选中状态存在cookie中，此处与redis中的数据进行合并更新，以免用户未登录状态下的isChecked丢失
        for (CartInfo cartInfo : cartInfoList) {
            for (CartInfo info : cartListFromCookie) {
                if (cartInfo.getSkuId().equals(info.getSkuId())){
                    if (info.getIsChecked().equals("1")){ //只更新未登录状态时选中的
                        //将cookie中的选中状态 合并
                        cartInfo.setIsChecked(info.getIsChecked());
                        //更新redis
                        checkCart(cartInfo.getSkuId(),info.getIsChecked(),userId);
                    }
                }
            }
        }

        return cartInfoList;
    }
    //更改redis中的购物车信息
    /*
    hset(user:[userId]:cart,skuId,CartInfoJson)
      key：“user:[userId]:cart”
     field:     [skuId]
     value:    CartInfo  (Json)
     */
    @Override
    public void checkCart(String skuId, String isChecked, String userId) {
        Jedis jedis = redisUtil.getJedis();
        //购物车主键 user:userId:cart
        String userCartKey = CartConst.USER_KEY_PREFIX + userId +CartConst.USER_CART_KEY_SUFFIX;
        //通过主键得到该userId下购物车内该skuId对应的信息
        String cartJson = jedis.hget(userCartKey, skuId);
        CartInfo cartInfo = JSON.parseObject(cartJson, CartInfo.class);
        cartInfo.setIsChecked(isChecked);
        String cartCheckdJson  = JSON.toJSONString(cartInfo);
        jedis.hset(userCartKey,skuId,cartCheckdJson);

        //redis 新增到已选中购物车  user:userId:checked
        //已选中购物车主键
        String cartCheckedKey = CartConst.USER_KEY_PREFIX + userId + CartConst.USER_CHECKED_KEY_SUFFIX;

        if (isChecked.equals("1")){
            //更新保存已经选中的
            jedis.hset(cartCheckedKey,skuId,cartCheckdJson);
        }else {
            //删除未选中的
            jedis.hdel(cartCheckedKey,skuId);
        }

        jedis.close();
    }

    @Override
    public List<CartInfo> getCartCheckedList(String userId) {
        Jedis jedis = redisUtil.getJedis();
        //获得redis中内选中购物车的主键 user:userId:checked
        String userCheckedKey = CartConst.USER_KEY_PREFIX + userId + CartConst.USER_CHECKED_KEY_SUFFIX;
        //获得redis中存的信息 是json形式
        List<String> cartCheckedList  = jedis.hvals(userCheckedKey);
        //json转为对象类型 存入集合 并返回
        List<CartInfo> newCartList = new ArrayList<>();
        for (String  cartJson : cartCheckedList) {
            CartInfo cartInfo = JSON.parseObject(cartJson, CartInfo.class);
            newCartList.add(cartInfo);
        }
        jedis.close();
        return newCartList;
    }

    /*
        购物车查询，在数据库中查找，并更新redis
     */
    public  List<CartInfo> loadCartCache(String userId){
        //cartInfo--购物车价格  ，实时价格 skuInfo.price
        List<CartInfo> cartInfoList = cartInfoMapper.selectCartListWithCurPrice(userId);
        if (cartInfoList==null &&cartInfoList.size()==0){
            return null;
        }
        //购物车redis 主键
        String userCartKey = CartConst.USER_KEY_PREFIX+userId+CartConst.USER_CART_KEY_SUFFIX;
        //存入redis
        Jedis jedis = redisUtil.getJedis();
        Map<String,String> map = new HashMap<>(cartInfoList.size());
        //将对象cartInfoList中的cartInfo转换为json格式，依次存入redis
        for (CartInfo cartInfo : cartInfoList) {
            String cartJson = JSON.toJSONString(cartInfo);
        // key 都是同一个，值会产生重复覆盖！
            map.put(cartInfo.getSkuId(),cartJson);
        }
        // 将java list - redis hash
        jedis.hmset(userCartKey,map);
        jedis.close();
        return  cartInfoList;
    }


}



