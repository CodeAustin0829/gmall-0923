package com.atguigu.gmall.order.vo;

import com.atguigu.gmall.oms.vo.OrderItemVo;
import com.atguigu.gmall.ums.entity.UserAddressEntity;
import lombok.Data;

import java.util.List;

/**
 * @Description 订单确认VO类
 * @Author Austin
 * @Date 2021/3/31
 */
@Data
public class OrderConfirmVo {

    // 收获地址列表，可以有多个收获地址，UserAddressEntity保存在ums微服务中
    private List<UserAddressEntity> addresses;

    // 送货清单，根据购物车页面传递过来的skuIds查询，点击标题的时候，可以跳转商品详情页，所以需要skuId属性
    private List<OrderItemVo> orderItems;

    // 用户的购物分信息，ums_user表中的integration字段
    private Integer bounds;

    // 防重的唯一标识，保证提交的幂等性，防止重复提交清单
    private String orderToken;
}
