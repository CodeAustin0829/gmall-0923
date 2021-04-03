package com.atguigu.gmall.wms.service;

import com.atguigu.gmall.common.bean.PageParamVo;
import com.atguigu.gmall.common.bean.PageResultVo;
import com.atguigu.gmall.wms.entity.WareSkuEntity;
import com.atguigu.gmall.wms.vo.SkuLockVo;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * 商品库存
 *
 * @author Austin
 * @email 985474500@qq.com
 * @date 2021-03-09 00:43:26
 */
public interface WareSkuService extends IService<WareSkuEntity> {

    PageResultVo queryPage(PageParamVo paramVo);

    /**
     * 提交订单时验证库存、锁定库存
     * 参数：用户点击提交订单，提交多个参数，使用Json数据来提交，使用@RequestBody+list集合来接受
     * 返回值：List<SkuLockVo>，参考京东，验证库存、锁定库存后，也要返回有集合，显示库存的状态
     */
    List<SkuLockVo> checkAndLock(List<SkuLockVo> lockVos);
}

