package com.atguigu.gmall.ums.service;

import com.atguigu.gmall.common.bean.PageParamVo;
import com.atguigu.gmall.common.bean.PageResultVo;
import com.atguigu.gmall.ums.entity.UserEntity;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * 用户表
 *
 * @author Austin
 * @email 985474500@qq.com
 * @date 2021-03-09 00:36:44
 */
public interface UserService extends IService<UserEntity> {

    PageResultVo queryPage(PageParamVo paramVo);

    /**
     * 注册和登录的用户数据校验
     */
    Boolean checkData(String data, Integer type);

    /**
     * 注册
     * 表单数据使用post请求，多个记录使用对象来接受
     * UserEntity类中，没有code属性，单独接收
     */
    void register(UserEntity userEntity, String code);

    /**
     * 登录
     * 查询用户，登录用户名、登录密码，在？后面进行传参
     */
    UserEntity queryUser(String loginName, String password);

}

