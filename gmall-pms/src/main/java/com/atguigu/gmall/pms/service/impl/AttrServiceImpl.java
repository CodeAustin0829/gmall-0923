package com.atguigu.gmall.pms.service.impl;

import com.atguigu.gmall.pms.entity.AttrEntity;
import org.springframework.stereotype.Service;

import java.util.List;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.atguigu.gmall.common.bean.PageResultVo;
import com.atguigu.gmall.common.bean.PageParamVo;

import com.atguigu.gmall.pms.mapper.AttrMapper;

import com.atguigu.gmall.pms.service.AttrService;


@Service("attrService")
public class AttrServiceImpl extends ServiceImpl<AttrMapper, AttrEntity> implements AttrService {

    @Override
    public PageResultVo queryPage(PageParamVo paramVo) {
        IPage<AttrEntity> page = this.page(
                paramVo.getPage(),
                new QueryWrapper<AttrEntity>()
        );

        return new PageResultVo(page);
    }

    //查询分类下的规格参数，用来录入sku相关信息
    @Override
    public List<AttrEntity> queryAttrByCIdOrTypeOrSearchType(Long cid, Integer type, Integer searchType) {
        QueryWrapper<AttrEntity> queryWrapper = new QueryWrapper<>();

        //（1）第一步：cid是在url中进行携带的，不可能为空，但是有可能等于0
        if (cid != 0) {
            //当cid等于0的时候，不用设定到查询条件当中，也就是queryWrapper为空，直接查询全部规格的参数即可
            //当cid不等于0时，才设定到查询条件当中
            queryWrapper.eq("category_id", cid);
        }

        //（2）第二步：当type不为空时，设定到查询条件当中
        if (type != null) {
            queryWrapper.eq("type", type);
        }

        //（3）第三步：当searchType不为空时，设定到查询条件当中
        if (searchType != null) {
            queryWrapper.eq("search_type", searchType);
        }
        //（4）调用IService的查询list的方法，将查询条件设定进去
        return this.list(queryWrapper);
    }

}