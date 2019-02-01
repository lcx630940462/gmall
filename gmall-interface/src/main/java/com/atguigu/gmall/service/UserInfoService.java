package com.atguigu.gmall.service;

import com.atguigu.gmall.bean.UserAddress;
import com.atguigu.gmall.bean.UserInfo;

import java.util.List;

public interface UserInfoService {
    //查询所有用户数据
    List<UserInfo> findAll();
    //根据用户id查询地址集合
    List<UserAddress> findUserAddressByUserId(String id);
    //通过用户输入的UserInfo信息 在数据库查询是否账号 密码匹配
    public UserInfo login(UserInfo userInfo);

    //通过userId从redis中查询userInfo
    UserInfo verify(String userId);

}
