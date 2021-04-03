package com.atguigu.gmall.pms.service;

import com.atguigu.gmall.pms.entity.SkuEntity;

import com.atguigu.gmall.common.bean.PageResultVo;
import com.atguigu.gmall.common.bean.PageParamVo;
import com.baomidou.mybatisplus.extension.service.IService;


/**
 * sku信息
 *
 * @author Austin
 * @email 985474500@qq.com
 * @date 2021-03-08 23:37:03
 */
public interface SkuService extends IService<SkuEntity> {

    PageResultVo queryPage(PageParamVo paramVo);
}

