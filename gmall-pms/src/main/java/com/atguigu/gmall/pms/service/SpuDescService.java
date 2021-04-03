package com.atguigu.gmall.pms.service;


import com.atguigu.gmall.pms.entity.SpuDescEntity;
import com.atguigu.gmall.pms.entity.vo.SpuVO;
import com.baomidou.mybatisplus.extension.service.IService;
import com.atguigu.gmall.common.bean.PageResultVo;
import com.atguigu.gmall.common.bean.PageParamVo;


/**
 * spu信息介绍
 *
 * @author Austin
 * @email 985474500@qq.com
 * @date 2021-03-08 23:37:04
 */
public interface SpuDescService extends IService<SpuDescEntity> {

    PageResultVo queryPage(PageParamVo paramVo);

    void saveSpuDesc(SpuVO spu, Long spuId);
}

