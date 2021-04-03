package com.atguigu.gmall.sms.service;

import com.atguigu.gmall.common.bean.PageParamVo;
import com.atguigu.gmall.common.bean.PageResultVo;
import com.atguigu.gmall.sms.entity.SkuBoundsEntity;
import com.atguigu.gmall.sms.vo.ItemSaleVo;
import com.atguigu.gmall.sms.vo.SkuSaleVo;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * 商品spu积分设置
 *
 * @author Austin
 * @email 985474500@qq.com
 * @date 2021-03-09 00:14:24
 */
public interface SkuBoundsService extends IService<SkuBoundsEntity> {

    PageResultVo queryPage(PageParamVo paramVo);

    /**
     * 新增sku的营销信息
     */
    void saveSkuSale(SkuSaleVo skuSaleVo);

    /**
     * 商品详情页接口六：根据skuId，查询sku的所有营销信息
     */
    List<ItemSaleVo> querySalesBySkuId(Long skuId);

}

