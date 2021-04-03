package com.atguigu.gmall.ums.service.impl;

import com.atguigu.gmall.common.bean.PageParamVo;
import com.atguigu.gmall.common.bean.PageResultVo;
import com.atguigu.gmall.ums.entity.UserEntity;
import com.atguigu.gmall.ums.mapper.UserMapper;
import com.atguigu.gmall.ums.service.UserService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.UUID;


@Service("userService")
public class UserServiceImpl extends ServiceImpl<UserMapper, UserEntity> implements UserService {

    @Override
    public PageResultVo queryPage(PageParamVo paramVo) {
        IPage<UserEntity> page = this.page(
                paramVo.getPage(),
                new QueryWrapper<UserEntity>()
        );

        return new PageResultVo(page);
    }

    /**
     * 注册和登录的用户数据校验
     */
    @Override
    public Boolean checkData(String data, Integer type) {
        // 2.创建查询条件对象
        QueryWrapper<UserEntity> queryWrapper = new QueryWrapper<>();

        // 3.设置查询条件，type=1，校验用户名；=2，校验手机；=3，校验邮箱
        switch (type) {
            case 1:
                queryWrapper.eq("username", data);
                break;
            case 2:
                queryWrapper.eq("phone", data);
                break;
            case 3:
                queryWrapper.eq("email", data);
                break;
            default:
                return null;
        }

        // 1.查询数字等于0，表示用户没有注册过
        return this.count(queryWrapper) == 0;
    }

    /**
     * 注册
     * 表单数据使用post请求，多个记录使用对象来接受
     * UserEntity类中，没有code属性，单独接收
     */
    @Override
    public void register(UserEntity userEntity, String code) {
        /**
         * 1.校验验证码，查询redis中的验证码和用户输入的code验证码比较
         */

        /**
         * 2.为密码生成盐
         *
         * 使用substring截取UUID中0-6位数作为盐，加到密码当中
         */
        String salt = UUID.randomUUID().toString().substring(0, 6);
        userEntity.setSalt(salt);

        /**
         * 3.为密码加盐加密
         *
         * 使用Apache提供的工具类，为加盐密码加密
         */
        userEntity.setPassword(DigestUtils.md5Hex(userEntity.getPassword() + salt));

        /**
         * 4.新增用户，写入数据库
         */
        userEntity.setLevelId(1L);
        userEntity.setNickname(userEntity.getUsername());
        userEntity.setSourceType(1);
        userEntity.setIntegration(1000);
        userEntity.setGrowth(1000);
        userEntity.setStatus(1);
        userEntity.setCreateTime(new Date());
        this.save(userEntity);

        /**
         * 5.删除redis中的验证码
         */
    }

    /**
     * 登录
     * 查询用户，登录用户名、登录密码，在？后面进行传参
     */
    @Override
    public UserEntity queryUser(String loginName, String password) {
        /**
         * 1.根据登录名查询用户信息（拿到盐）
         *
         * 用户可能以用户名、手机号、邮箱当作登录用户名，所以都要当作条件查询
         */
        UserEntity userEntity = this.getOne(new QueryWrapper<UserEntity>()
                .eq("username", loginName)
                .or()
                .eq("phone", loginName)
                .or()
                .eq("email", loginName));

        /**
         * 2.判断用户是否为空
         */
        if (userEntity == null) {
            return null;
        }

        /**
         * 3.对用户输入的密码加盐加密，并和数据库中的密码进行比较
         */
        // 对用户输入的密码加盐加密
        String userPassWord = DigestUtils.md5Hex(password + userEntity.getSalt());
        // 获取数据库中的密码
        String DBPassWord = userEntity.getPassword();
        // 进行判断比较
        if (!StringUtils.equals(userPassWord, DBPassWord)) {
            return null;
        }
        /**
         * 4.返回用户信息
         */
        return userEntity;
    }
}
