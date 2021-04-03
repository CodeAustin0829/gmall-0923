package com.atguigu.gmall.pms.service;

import com.atguigu.gmall.common.bean.PageParamVo;
import com.atguigu.gmall.common.bean.PageResultVo;
import com.atguigu.gmall.pms.entity.SpuAttrValueEntity;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;


/**
 * spu属性值
 *
 * @author Austin
 * @email 985474500@qq.com
 * @date 2021-03-08 23:37:04
 */
public interface SpuAttrValueService extends IService<SpuAttrValueEntity> {

    PageResultVo queryPage(PageParamVo paramVo);

    /**
     * ES搜索之根据分类Id、spuId查询检索类型的基本属性及值
     */
    List<SpuAttrValueEntity> querySpuAttrValueByCidAndSpuId(Long cid, Long spuId);
}

