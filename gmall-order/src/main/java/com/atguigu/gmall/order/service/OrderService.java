package com.atguigu.gmall.order.service;

import com.atguigu.gmall.cart.entity.Cart;
import com.atguigu.gmall.common.bean.ResponseVo;
import com.atguigu.gmall.common.exception.OrderException;
import com.atguigu.gmall.oms.vo.OrderItemVo;
import com.atguigu.gmall.order.entity.UserInfo;
import com.atguigu.gmall.order.feign.*;
import com.atguigu.gmall.order.interceptor.LoginInterceptor;
import com.atguigu.gmall.order.vo.OrderConfirmVo;
import com.atguigu.gmall.pms.entity.SkuAttrValueEntity;
import com.atguigu.gmall.pms.entity.SkuEntity;
import com.atguigu.gmall.sms.vo.ItemSaleVo;
import com.atguigu.gmall.ums.entity.UserAddressEntity;
import com.atguigu.gmall.ums.entity.UserEntity;
import com.atguigu.gmall.wms.entity.WareSkuEntity;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * @Description
 * @Author Austin
 * @Date 2021/3/31
 */
@Service
public class OrderService {

    @Autowired
    private GmallPmsClient gmallPmsClient;

    @Autowired
    private GmallSmsClient gmallSmsClient;

    @Autowired
    private GmallUmsClient gmallUmsClient;

    @Autowired
    private GmallCartClient gmallCartClient;

    @Autowired
    private GmallWmsClient gmallWmsClient;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    private static final String KEY_PREFIX = "order:token:";


    /**
     * 处理订单结算页数据查询接口
     */
    public OrderConfirmVo confirm() {
        // 1.第一步：创建订单确认VO类的对象，用来封装查询到的数据
        OrderConfirmVo orderConfirmVo = new OrderConfirmVo();

        // 2.2.从拦截器中获取用户id
        UserInfo userInfo = LoginInterceptor.getUserInfo();
        Long userId = userInfo.getUserId();

        // 2.第二步：为OrderConfirmVo的属性List<UserAddressEntity> addresses赋值
        // 2.1.远程调用ums微服务，查询ums_user_address表
        ResponseVo<List<UserAddressEntity>> userAddressResponseVo =
                this.gmallUmsClient.queryAddressesByUserId(userId);
        // 2.3.从ResponseVo中，获取地址集合
        List<UserAddressEntity> userAddressEntities = userAddressResponseVo.getData();
        // 2.4.将地址集合，赋值给属性addresses
        orderConfirmVo.setAddresses(userAddressEntities);

        /*
        3.第三步：为OrderConfirmVo的属性List<OrderItemVo> items赋值
        除了skuId、count，其它属性都进行远程调用查询
        虽然购物车也有，但不是最实时的
         */
        // 3.1.远程调用gmall-cart微服务，查询用户选中的是哪些商品进行结算，并得到商品的skuId、count
        ResponseVo<List<Cart>> cartResponseVo = this.gmallCartClient.queryCheckedCarts(userId);
        // 3.1.1.得到用户选中商品的购物车集合
        List<Cart> carts = cartResponseVo.getData();
        // 3.1.2.对购物车集合进行判断，当没有选中的时候，抛出异常
        if (CollectionUtils.isEmpty(carts)) {
            throw new OrderException("您没有选中的购物车记录！");
        }
        // 3.2.查询所有字段后，最终的目的，就是将List<Cart> carts，转换为List<OrderItemVo> itemVos，然后设置给OrderConfirmVo属性中
        List<OrderItemVo> itemVos = carts.stream().map(cart -> { // 只取skuId count
            OrderItemVo orderItemVo = new OrderItemVo();

            orderItemVo.setSkuId(cart.getSkuId());
            orderItemVo.setCount(cart.getCount().intValue());

            // 根据skuId查询sku
            ResponseVo<SkuEntity> skuEntityResponseVo = this.gmallPmsClient.querySkuById(cart.getSkuId());
            SkuEntity skuEntity = skuEntityResponseVo.getData();
            if (skuEntity != null) {
                orderItemVo.setDefaultImage(skuEntity.getDefaultImage());
                orderItemVo.setTitle(skuEntity.getTitle());
                orderItemVo.setPrice(skuEntity.getPrice());
                orderItemVo.setWeight(new BigDecimal(skuEntity.getWeight()));
            }


            // 根据skuId查询该商品的销售属性
            ResponseVo<List<SkuAttrValueEntity>> saleAttrResponseVo = this.gmallPmsClient.querySkuAttrValuesBySkuId(cart.getSkuId());
            List<SkuAttrValueEntity> skuAttrValueEntities = saleAttrResponseVo.getData();
            orderItemVo.setSaleAttrs(skuAttrValueEntities);

            // 根据skuId查询库存
            ResponseVo<List<WareSkuEntity>> wareResponseVo = this.gmallWmsClient.queryWareSkuBySkuId(cart.getSkuId());
            List<WareSkuEntity> wareSkuEntities = wareResponseVo.getData();
            if (!CollectionUtils.isEmpty(wareSkuEntities)) {
                orderItemVo.setStore(wareSkuEntities.stream().anyMatch(wareSkuEntity -> wareSkuEntity.getStock() - wareSkuEntity.getStockLocked() > 0));
            }

            // 根据skuId查询营销信息
            ResponseVo<List<ItemSaleVo>> salesResponseVo = this.gmallSmsClient.querySalesBySkuId(cart.getSkuId());
            List<ItemSaleVo> itemSaleVos = salesResponseVo.getData();
            orderItemVo.setSales(itemSaleVos);

            return orderItemVo;
        }).collect(Collectors.toList());
        orderConfirmVo.setOrderItems(itemVos);


        // 4.第四步：为OrderConfirmVo的属性Integer bounds赋值
        ResponseVo<UserEntity> userEntityResponseVo = this.gmallUmsClient.queryUserById(userId);
        UserEntity userEntity = userEntityResponseVo.getData();
        if (userEntity != null) {
            orderConfirmVo.setBounds(userEntity.getIntegration());
        }

        // 5.第五步：为OrderConfirmVo的属性String orderToKen赋值，浏览器保存一份、redis保存一份
        String orderToken = IdWorker.getIdStr();
        orderConfirmVo.setOrderToken(orderToken);
        this.stringRedisTemplate.opsForValue().set(KEY_PREFIX + orderToken, orderToken, 1, TimeUnit.HOURS);

        // 6.第六步：返回封装好查询数据的OrderConfirmVo
        return orderConfirmVo;
    }

}
