package com.atguigu.gmall.ums.api;

import com.atguigu.gmall.common.bean.ResponseVo;
import com.atguigu.gmall.ums.entity.UserAddressEntity;
import com.atguigu.gmall.ums.entity.UserEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * @Description
 * @Author Austin
 * @Date 2021/3/26
 */
public interface GmallUmsApi {

    /**
     * 登录
     * 查询用户，登录用户名、登录密码，在？后面进行传参
     */
    @GetMapping("ums/user/query")
    public ResponseVo<UserEntity> queryUser(@RequestParam("loginName") String loginName, @RequestParam("password") String password);

    /**
     * 注册
     * 表单数据使用post请求，多个记录使用对象来接受
     * UserEntity类中，没有code属性，单独接收
     */
    @PostMapping("ums/user/register")
    public ResponseVo register(UserEntity userEntity, @RequestParam("code") String code);

    /**
     * 注册和登录的用户数据校验
     */
    @GetMapping("ums/user/check/{data}/{type}")
    public ResponseVo<Boolean> checkData(@PathVariable("data") String data, @PathVariable("type") Integer type);

    /**
     * 订单确认页所需接口：根据用户id，查询收货地址
     */
    @GetMapping("ums/useraddress/user/{userId}")
    public ResponseVo<List<UserAddressEntity>> queryAddressesByUserId(@PathVariable("userId") Long userId);

    /**
     * 订单确认页所需接口：根据用户id，查询用户积分[用户信息中包含积分信息]
     */
    @GetMapping("ums/user/{id}")
    public ResponseVo<UserEntity> queryUserById(@PathVariable("id") Long id);
}
