package com.atguigu.gmall.pms.service;


import com.atguigu.gmall.pms.entity.SpuEntity;
import com.atguigu.gmall.pms.entity.vo.SpuVO;
import com.baomidou.mybatisplus.extension.service.IService;
import com.atguigu.gmall.common.bean.PageResultVo;
import com.atguigu.gmall.common.bean.PageParamVo;


/**
 * spu信息
 *
 * @author Austin
 * @email 985474500@qq.com
 * @date 2021-03-08 23:37:03
 */
public interface SpuService extends IService<SpuEntity> {

    PageResultVo queryPage(PageParamVo paramVo);

    //查询商品列表
    PageResultVo querySpuByCategoryIdOrKey(Long categoryId, PageParamVo pageParamVo);

    // 大保存：保存spu、sku、营销相关信息
    void bigSave(SpuVO spu);

//    void saveSpuDesc(SpuVO spu, Long spuId); 为了测试事务传播行为，放在SpuDescService中
}

