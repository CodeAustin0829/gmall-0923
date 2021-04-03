package com.atguigu.gmall.oms.service;

import com.atguigu.gmall.common.bean.PageParamVo;
import com.atguigu.gmall.common.bean.PageResultVo;
import com.atguigu.gmall.oms.entity.OrderEntity;
import com.atguigu.gmall.oms.vo.OrderSubmitVo;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * 订单
 *
 * @author Austin
 * @email 985474500@qq.com
 * @date 2021-03-09 00:27:53
 */
public interface OrderService extends IService<OrderEntity> {

    PageResultVo queryPage(PageParamVo paramVo);

    /**
     * 提交订单
     */
    OrderEntity saveOrder(OrderSubmitVo submitVo, Long userId);
}

