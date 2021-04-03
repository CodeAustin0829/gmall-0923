package com.atguigu.gmall.pms.service;

import com.atguigu.gmall.pms.entity.CategoryBrandEntity;

import com.atguigu.gmall.common.bean.PageResultVo;
import com.atguigu.gmall.common.bean.PageParamVo;
import com.baomidou.mybatisplus.extension.service.IService;


/**
 * 品牌分类关联
 *
 * @author Austin
 * @email 985474500@qq.com
 * @date 2021-03-08 23:37:03
 */
public interface CategoryBrandService extends IService<CategoryBrandEntity> {

    PageResultVo queryPage(PageParamVo paramVo);
}

