package com.atguigu.gmall.cart.service;

import com.atguigu.gmall.cart.entity.Cart;
import com.atguigu.gmall.cart.mapper.CartMapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * @Description 异步加入购物车
 * @Author Austin
 * @Date 2021/3/28
 */
@Service
public class CartAsyncService {

    @Autowired
    private CartMapper cartMapper;

    /**
     * 更新购物车的异步执行方法
     *
     * @param userId
     * @param cart
     */
    @Async
    public void updateCartByUserIdAndSkuId(String userId, Cart cart) {
        this.cartMapper.update(cart, new QueryWrapper<Cart>()
                .eq("user_id", cart.getUserId())
                .eq("sku_id", cart.getSkuId()));
    }

    /**
     * 添加购物车的异步执行方法
     *
     * @param cart
     */
    @Async
    public void insertCart(Cart cart) {
        this.cartMapper.insert(cart);
    }

    /**
     * 删除未登录购物车的异步执行方法
     *
     * @param userId
     */
    // 数据库表的字段是userId，也就是userKey（未登录）、userId（登录）的统称，此处根据统称userId来接受userKey，进行删除即可
    public void deleteUnLoginCarts(String userId) {
        this.cartMapper.delete(new QueryWrapper<Cart>().eq("user_id", userId));
    }

    /**
     * 更新商品数量
     *
     * @param userId
     * @param skuId
     * @param cart
     */
    public void updateCartBySkuId(String userId, Long skuId, Cart cart) {
        this.cartMapper.update(cart, new UpdateWrapper<Cart>()
                .eq("user_id", userId)
                .eq("sku_id", skuId));
    }

    /**
     * 删除购物车
     * - 请求方式：Post
     * - 请求路径：/deleteCart?skuId=30
     * - 请求参数：skuId
     * - 返回结果：无
     */
    public void deleteCartByUserIdAndSkuId(String userId, Long skuId) {
        this.cartMapper.delete(new QueryWrapper<Cart>().eq("user_id", userId).eq("sku_id", skuId));
    }
}
