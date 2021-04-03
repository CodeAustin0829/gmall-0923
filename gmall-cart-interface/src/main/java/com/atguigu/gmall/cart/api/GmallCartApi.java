package com.atguigu.gmall.cart.api;

import com.atguigu.gmall.cart.entity.Cart;
import com.atguigu.gmall.common.bean.ResponseVo;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;

/**
 * @Description
 * @Author Austin
 * @Date 2021/3/31
 */
public interface GmallCartApi {

    /**
     * 订单确认页所需接口：获取登录用户勾选的购物车
     * ---------------------------------------------------------------
     * 需要传递参数userId来进行，虽然在拦截中将请求拦截下拉，从Cookie中获取到用户信息
     * 但是前提是浏览器发过来的请求，浏览器是携带coolie的
     * 而gmall-order微服务远程调用，是没有coolie的，拦截器无法从coolie中获取用户信息
     * 所以还需要传递userId过来
     */
    @GetMapping("check/{userId}")
    @ResponseBody
    public ResponseVo<List<Cart>> queryCheckedCarts(@PathVariable("userId") Long userId);
}
