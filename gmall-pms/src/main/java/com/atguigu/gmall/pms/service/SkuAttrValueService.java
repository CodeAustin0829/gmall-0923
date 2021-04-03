package com.atguigu.gmall.pms.service;

import com.atguigu.gmall.common.bean.PageParamVo;
import com.atguigu.gmall.common.bean.PageResultVo;
import com.atguigu.gmall.pms.entity.SkuAttrValueEntity;
import com.atguigu.gmall.pms.entity.vo.SaleAttrValueVo;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;
import java.util.Map;


/**
 * sku销售属性&值
 *
 * @author Austin
 * @email 985474500@qq.com
 * @date 2021-03-08 23:37:03
 */
public interface SkuAttrValueService extends IService<SkuAttrValueEntity> {

    PageResultVo queryPage(PageParamVo paramVo);

    /**
     * ES搜索之根据分类Id、skuId查询检索类型的销售属性及值
     */
    List<SkuAttrValueEntity> querySkuAttrValueByCidAndSkuId(Long cid, Long skuId);

    /**
     * 商品详情页接口八：查询sku信息后得到spuId，根据spuId查询spu下的所有销售属性
     */
    List<SaleAttrValueVo> querySkuAttrValuesBySpuId(Long spuId);

    /**
     * 商品详情页接口十：根据sku中的spuId查询spu下所有sku，以及所有sku的销售属性组合与skuId映射关系
     */
    Map<String, Object> querySaleAttrsMappingSkuIdBySpuId(Long spuId);
}

