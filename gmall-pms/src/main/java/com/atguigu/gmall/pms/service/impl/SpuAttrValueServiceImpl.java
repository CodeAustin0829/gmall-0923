package com.atguigu.gmall.pms.service.impl;

import com.atguigu.gmall.common.bean.PageParamVo;
import com.atguigu.gmall.common.bean.PageResultVo;
import com.atguigu.gmall.pms.entity.AttrEntity;
import com.atguigu.gmall.pms.entity.SpuAttrValueEntity;
import com.atguigu.gmall.pms.mapper.AttrMapper;
import com.atguigu.gmall.pms.mapper.SkuMapper;
import com.atguigu.gmall.pms.mapper.SpuAttrValueMapper;
import com.atguigu.gmall.pms.service.SpuAttrValueService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.stream.Collectors;


@Service("spuAttrValueService")
public class SpuAttrValueServiceImpl extends ServiceImpl<SpuAttrValueMapper, SpuAttrValueEntity> implements SpuAttrValueService {

    @Autowired
    private AttrMapper attrMapper;
    @Autowired
    private SkuMapper skuMapper;

    @Override
    public PageResultVo queryPage(PageParamVo paramVo) {
        IPage<SpuAttrValueEntity> page = this.page(
                paramVo.getPage(),
                new QueryWrapper<SpuAttrValueEntity>()
        );

        return new PageResultVo(page);
    }

    /**
     * ES搜索之根据分类Id、spuId查询检索类型的基本属性及值
     */
    @Override
    public List<SpuAttrValueEntity> querySpuAttrValueByCidAndSpuId(Long cid, Long spuId) {
        //（1）第一步：根据分布查询的SQL语句分析，查询出pms_attr表检索类型的规格参数【两个eq】，返回的是AttrEntity的list集合
        List<AttrEntity> attrEntityList = this.attrMapper.selectList(new QueryWrapper<AttrEntity>().eq("category_id", cid).eq("search_type", 1));
        //判断获取到的集合是否为空，如果为空，直接结束当前方法，不执行下面的代码
        if (CollectionUtils.isEmpty(attrEntityList)) {
            return null;
        }
        //（2）第二步：但是第二个查询SQL语句，需要的是attrEntity中的id集合【in (4,5,6,8,9)】，使用stream表达式进行转换
        /*
        List<Long> attrIds = attrEntityList.stream().map(AttrEntity -> AttrEntity.getId()).collect(Collectors.toList());
        可以简写成::
         */
        List<Long> attrIds = attrEntityList.stream().map(AttrEntity::getId).collect(Collectors.toList());
        //（3）第三步：执行第二个SQL查询语句，查询出基本检索类型的规格参数及值
        List<SpuAttrValueEntity> spuAttrValueEntityList = this.list(new QueryWrapper<SpuAttrValueEntity>().eq("spu_id", spuId).in("attr_id", attrIds));
        return spuAttrValueEntityList;
    }
}