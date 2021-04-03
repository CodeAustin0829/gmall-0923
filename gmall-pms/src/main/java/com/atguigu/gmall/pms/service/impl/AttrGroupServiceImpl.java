package com.atguigu.gmall.pms.service.impl;


import com.atguigu.gmall.common.bean.PageParamVo;
import com.atguigu.gmall.common.bean.PageResultVo;
import com.atguigu.gmall.pms.entity.AttrEntity;
import com.atguigu.gmall.pms.entity.AttrGroupEntity;
import com.atguigu.gmall.pms.entity.SkuAttrValueEntity;
import com.atguigu.gmall.pms.entity.SpuAttrValueEntity;
import com.atguigu.gmall.pms.entity.vo.AttrValueVo;
import com.atguigu.gmall.pms.entity.vo.GroupVo;
import com.atguigu.gmall.pms.entity.vo.ItemGroupVo;
import com.atguigu.gmall.pms.mapper.AttrGroupMapper;
import com.atguigu.gmall.pms.mapper.AttrMapper;
import com.atguigu.gmall.pms.mapper.SkuAttrValueMapper;
import com.atguigu.gmall.pms.mapper.SpuAttrValueMapper;
import com.atguigu.gmall.pms.service.AttrGroupService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;


@Service("attrGroupService")
public class AttrGroupServiceImpl extends ServiceImpl<AttrGroupMapper, AttrGroupEntity> implements AttrGroupService {

    @Autowired
    private AttrMapper attrMapper;
    @Autowired
    private SpuAttrValueMapper spuAttrValueMapper;
    @Autowired
    private SkuAttrValueMapper skuAttrValueMapper;

    @Override
    public PageResultVo queryPage(PageParamVo paramVo) {
        IPage<AttrGroupEntity> page = this.page(
                paramVo.getPage(),
                new QueryWrapper<AttrGroupEntity>()
        );

        return new PageResultVo(page);
    }

    //商品新增之查询分类下的分组及其规格参数
    @Override
    public List<GroupVo> queryGroupVoByCid(Long cid) {
        /**
         * 方式一：SQL联表查询
         * （1）根据pms_attr_group表的分类id——category_id字段，查询分类下的分组
         * （2）根据pms_attr表的分组id——group_id字段，查询分组下面的规格参数
         * 由于pms_attr也有category_id字段，所以可以联表查询，只需要一个即可
         * 但是在实际开发中，不建议联表查询，因为会影响查询性能
         * -----------------------------------------------------------------
         * 方式二：Java分步查询
         * （1）第一步：在Java代码中，先查询出分类下的分组
         * （2）第二步：遍历所有分组，根据分组id，查询分组下面的规格参数
         */

        //（1）第一步：查询分类下的所有分组【调用基类IService的方法即可】，条件是前端传过来的cid，等于数据库中的category_id字段
        List<AttrGroupEntity> attrGroupEntities = this.list(new QueryWrapper<AttrGroupEntity>().eq("category_id", cid));


//        GroupVo groupVo = new GroupVo();
//        （2）第二步：遍历所有分组，获取分组id，根据分组id，查询分组下面的规格参数
//        ① 从集合数组List<AttrGroupEntity> attrGroupEntities中，遍历出每一个AttrGroupEntity对象，赋值给变量attrGroupEntity
//        for (AttrGroupEntity attrGroupEntity : attrGroupEntities) {
//            //② 获取分组id——groupId
//            Long groupId = attrGroupEntity.getId();
//            /*
//            ③ 根据分组id，查询每个分组下面的规格参数，
//            条件1：是pms_attr表的分组id——group_id字段，等于遍历获取到的分组id
//            条件2：只需查询出每个分组下的通用属性就可以了（不需要销售属性），也就是type=1
//             */
//            List<AttrEntity> attrEntityList =
//                    this.attrMapper.selectList(new QueryWrapper<AttrEntity>().eq("group_id", groupId).eq("type", 1));
//            //④ 将获取到List<AttrEntity> attrEntityList，封装到GroupVo类的attrEntities属性中，
//            // 同时将AttrGroupEntity类的属性，对拷到GroupVo类中
//
//
//            BeanUtils.copyProperties(attrGroupEntity, groupVo);
//            groupVo.setAttrEntities(attrEntityList);
////            卡住了，这样得到的只是一个groupVo对象，而返回值需要一个list集合
//
//        }
//
//        //（3）第三步：将groupVo返回去前端页面
//        return groupVo;
//    }
        //使用stream流进行改善
        // 查询出每组下的规格参数
        return attrGroupEntities.stream().map(attrGroupEntity -> {
            GroupVo groupVo = new GroupVo();
            BeanUtils.copyProperties(attrGroupEntity, groupVo);
            // 查询规格参数，只需查询出每个分组下的通用属性就可以了（不需要销售属性）
            List<AttrEntity> attrEntities = this.attrMapper.selectList(new QueryWrapper<AttrEntity>().eq("group_id", attrGroupEntity.getId()).eq("type", 1));
            groupVo.setAttrEntities(attrEntities);
            return groupVo;
        }).collect(Collectors.toList());

    }

    /**
     * 商品详情页接口十二：根据分类id、spuId及skuId，查询分组及组下的规格参数值
     * --------------------------------------------------------------------------
     * sql的实现步骤：
     * -- (1)根据分类id，查询出分组信息
     * SELECT * FROM pms_attr_group WHERE category_id = 225;
     * -- (2)遍历分组，得到分组id，根据分组id，查询出每个组下的规格参数及值
     * SELECT * FROM pms_attr WHERE group_id = 1;
     * -- (3)查询基本属性规格参数及值：根据attrId、spuId来进行查询
     * SELECT * FROM pms_spu_attr_value WHERE attr_id in (1,2,3) AND spu_id = 29;
     * -- (4)查询销售属性规格参数及值：根据attrId、skuId来进行查询
     * SELECT * FROM pms_sku_attr_value WHERE attr_id in (1,2,3) AND sku_id = 29;
     * --------------------------------------------------------------------------
     * 【思路理解】
     * 1.第一个sql语句，只是查询出每个分类下面的规格参数分组信息，查询的是pms_attr_group表，
     * 例如分类id为225的，一共有9个规格参数分组，但是，每个规格参数分组下面还有具体的
     * 规格参数
     * 2.所以还要根据分组的id，查询出每个分组下面的具体规格参数和值，具体的信息，查询的是
     * pms_attr表，例如在分类id=225的9个分组里面，分组id=1的，一共有3个具体的规格参数
     * 3.但是，这3个规格参数所对应的值，又分为两类
     * 当type=1时，对应的是基本属性，数据都存放在pms_spu_attr_value表中
     * 当type=2时，对应的是销售属性，数据都存放在pms_sku_attr_value表中
     * 4.所以，还要结合pms_attr里面的id，也就是attr_id，去查对应的值，
     * 当查的基本属性就根据attrId、spuId来进行查询
     * 当查的销售属性就根据attrId、skuId来进行查询
     * 5.为什么还要根据spuId、skuId来查呢？
     * 因为如果只是根据attr_id来查，如
     * SELECT * FROM pms_spu_attr_value WHERE attr_id in (1,2,3)
     * 查询出来的数据，就是查询attr_id=1,2,3的所有与spu相关的规格参数及值的数据了
     * 所以，还需要spuId来锁定到，要查询的是具体哪一个spu下面的规格参数及值
     * skuId同理
     * 6.总结，四个sql语句查询出来的数据，最终都属于分组下面的数据，也就是
     * 分类 → 分组 → 规格参数及值 → 销售属性规格参数及值
     * → 基本属性规格参数及值
     * 相当于，如果是多表联查，那最底层参考的表(也就是基于什么表的数据联查)就是pms_attr_group
     * 所以在代码实现的时候，只需要将
     * pms_attr_group查出来的集合List<AttrGroupEntity>
     * 转换为集合List<ItemGroupVo>即可
     */
    @Override
    public List<ItemGroupVo> queryGroupWithAttrValuesByCidAndSpuIdAndSkuId(Long cid, Long spuId, Long skuId) {
        // 1.第一步：根据分类id，查询出分组信息
        List<AttrGroupEntity> attrGroupEntityList = this.list(new QueryWrapper<AttrGroupEntity>().eq("category_id", cid));

        // 1.1.对分组集合进行判空，如果为空，直接return结束
        if (CollectionUtils.isEmpty(attrGroupEntityList)) {
            return null;
        }

        // 5.第五步：将分组集合List<AttrGroupEntity>，转换为List<ItemGroupVo>，并将结果返回
        return attrGroupEntityList.stream().map(attrGroupEntity -> {

            // 5.1.创建ItemGroupVo对象，接收转换
            ItemGroupVo itemGroupVo = new ItemGroupVo();

            // 5.3.设置属性groupId
            itemGroupVo.setGroupId(attrGroupEntity.getId());

            // 5.4.设置属性groupName
            itemGroupVo.setGroupName(attrGroupEntity.getName());

            /*
             5.5.设置属性List<AttrValueVo> attrValues
             需要先查询每个组下的规格参数及值，也就是执行第二步
             然后
             查询基本属性规格参数及值，设置到List<AttrValueVo> attrValues中，也就是执行第三步
             查询销售属性规格参数及值，设置到List<AttrValueVo> attrValues中，也就是执行第四步
             */
            // 2.第二步：遍历分组，得到分组id，根据分组id，查询出每个组下的规格参数及值
            List<AttrEntity> attrEntityList =
                    this.attrMapper.selectList(new QueryWrapper<AttrEntity>().eq("group_id", attrGroupEntity.getId()));

            // 2.1.对attrEntityList进行判空，当集合不为空时，才执行下面步骤
            if (!CollectionUtils.isEmpty(attrEntityList)) {
                // 2.2.取出分组集合中的规格参数id集合，以便执行第二步、第三步
                List<Long> attrIdList = attrEntityList.stream().map(AttrEntity::getId).collect(Collectors.toList());

                // 3.第三步：查询基本属性规格参数及值：根据attrId、spuId来进行查询
                List<SpuAttrValueEntity> spuAttrValueEntityList =
                        this.spuAttrValueMapper.selectList(new QueryWrapper<SpuAttrValueEntity>().eq("spu_id", spuId).in("attr_id", attrIdList));

                // 4.第四步：查询销售属性规格参数及值：根据attrId、skuId来进行查询
                List<SkuAttrValueEntity> skuAttrValueEntityList =
                        this.skuAttrValueMapper.selectList(new QueryWrapper<SkuAttrValueEntity>().eq("sku_id", skuId).in("attr_id", attrIdList));

                List<AttrValueVo> attrValueVoList = new ArrayList<>();

                // 5.5.1.将List<SpuAttrValueEntity> spuAttrValueEntityList(先判空再设置)，设置到属性List<AttrValueVo> attrValues中
                if (!CollectionUtils.isEmpty(spuAttrValueEntityList)) {
                    // 属性设置
                    List<AttrValueVo> spuAttrValueVos = spuAttrValueEntityList.stream().map(spuAttrValueEntity -> {
                        AttrValueVo attrValueVo = new AttrValueVo();
                        BeanUtils.copyProperties(spuAttrValueEntity, attrValueVo);
                        return attrValueVo;
                    }).collect(Collectors.toList());
                    // 将attrValueVo对象批量加入到list集合中
                    attrValueVoList.addAll(spuAttrValueVos);
                }

                // 5.5.2.将List<SkuAttrValueEntity> skuAttrValueEntityList(先判空再设置)，设置到属性List<AttrValueVo> attrValues中
                if (!CollectionUtils.isEmpty(skuAttrValueEntityList)) {
                    // 属性设置
                    List<AttrValueVo> skuAttrValueVos = skuAttrValueEntityList.stream().map(skuAttrValueEntity -> {
                        AttrValueVo attrValueVo = new AttrValueVo();
                        BeanUtils.copyProperties(skuAttrValueEntity, attrValueVo);
                        return attrValueVo;
                    }).collect(Collectors.toList());
                    // 将attrValueVo对象批量加入到list集合中
                    attrValueVoList.addAll(skuAttrValueVos);
                }

                // 5.5.3.设置属性List<AttrValueVo> attrValues
                itemGroupVo.setAttrValues(attrValueVoList);
            }
            // 5.2.将ItemGroupVo对象返回到stream()中的新List集合中
            return itemGroupVo;
        }).collect(Collectors.toList());
    }
}