package com.atguigu.gmall.auth.service;

import com.atguigu.gmall.auth.config.JwtProperties;
import com.atguigu.gmall.auth.feign.GmallUmsClient;
import com.atguigu.gmall.common.bean.ResponseVo;
import com.atguigu.gmall.common.exception.UserException;
import com.atguigu.gmall.common.utils.CookieUtils;
import com.atguigu.gmall.common.utils.IpUtils;
import com.atguigu.gmall.common.utils.JwtUtils;
import com.atguigu.gmall.ums.entity.UserEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;

/**
 * @Description
 * @Author Austin
 * @Date 2021/3/26
 */
@Service
@EnableConfigurationProperties({JwtProperties.class}) //激活使用配置类
public class AuthService {

    @Autowired
    private JwtProperties jwtProperties;

    @Autowired
    private GmallUmsClient gmallUmsClient;

    /**
     * 处理用户登录的请求
     */
    public void login(String loginName, String password, HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) {
        try {
            // 1.调用远程接口查询登录名和密码是否正确
            ResponseVo<UserEntity> userEntityResponseVo =
                    this.gmallUmsClient.queryUser(loginName, password);
            UserEntity userEntity = userEntityResponseVo.getData();

            // 2.判断用户信息是否为空
            if (userEntity == null) {
                throw new UserException("用户名或者密码有误！");
            }

            // 3.组装载荷信息：用户id、用户名，在JwtUtil工具类中，载荷封装在map中
            HashMap<String, Object> map = new HashMap<>();
            map.put("userId", userEntity.getId());
            map.put("userName", userEntity.getUsername());

            // 4. 为了防止jwt被别人盗取，载荷中加入用户ip地址，使用工具类
            map.put("ip", IpUtils.getIpAddressAtService(httpServletRequest));

            // 5. 制作jwt类型的token信息
            String token = JwtUtils.generateToken(map, this.jwtProperties.getPrivateKey(), this.jwtProperties.getExpire());

            // 6. 把jwt放入cookie中
            CookieUtils.setCookie(httpServletRequest, httpServletResponse, this.jwtProperties.getCookieName(),
                    token, this.jwtProperties.getExpire() * 60);

            // 7.用户昵称放入cookie中，方便页面展示昵称
            CookieUtils.setCookie(httpServletRequest, httpServletResponse,
                    this.jwtProperties.getUnick(), userEntity.getNickname(),
                    this.jwtProperties.getExpire() * 60);
        } catch (Exception e) {
            e.printStackTrace();
            throw new UserException("用户名或者密码出错！");
        }

    }
}
