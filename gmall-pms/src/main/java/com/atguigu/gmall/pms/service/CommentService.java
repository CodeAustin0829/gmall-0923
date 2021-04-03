package com.atguigu.gmall.pms.service;

import com.atguigu.gmall.pms.entity.CommentEntity;

import com.atguigu.gmall.common.bean.PageResultVo;
import com.atguigu.gmall.common.bean.PageParamVo;
import com.baomidou.mybatisplus.extension.service.IService;


/**
 * 商品评价
 *
 * @author Austin
 * @email 985474500@qq.com
 * @date 2021-03-08 23:37:03
 */
public interface CommentService extends IService<CommentEntity> {

    PageResultVo queryPage(PageParamVo paramVo);
}

