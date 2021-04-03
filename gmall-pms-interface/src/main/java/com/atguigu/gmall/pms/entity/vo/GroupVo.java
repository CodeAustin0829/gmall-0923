package com.atguigu.gmall.pms.entity.vo;


import com.atguigu.gmall.pms.entity.AttrEntity;
import com.atguigu.gmall.pms.entity.AttrGroupEntity;
import lombok.Data;

import java.util.List;

/**
 * @Description 商品新增之查询分类下的分组及其规格参数 对应的封装类 GroupVo
 * @Author Austin
 * @Date 2021/3/10
 */
@Data
public class GroupVo extends AttrGroupEntity {

    //新增属性，attrEntities里面封装的是AttrEntity的list集合
    private List<AttrEntity> attrEntities;
}
