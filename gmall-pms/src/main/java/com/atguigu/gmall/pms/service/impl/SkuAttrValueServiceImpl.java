package com.atguigu.gmall.pms.service.impl;

import com.atguigu.gmall.common.bean.PageParamVo;
import com.atguigu.gmall.common.bean.PageResultVo;
import com.atguigu.gmall.pms.entity.AttrEntity;
import com.atguigu.gmall.pms.entity.SkuAttrValueEntity;
import com.atguigu.gmall.pms.entity.SkuEntity;
import com.atguigu.gmall.pms.mapper.AttrMapper;
import com.atguigu.gmall.pms.mapper.SkuAttrValueMapper;
import com.atguigu.gmall.pms.mapper.SkuMapper;
import com.atguigu.gmall.pms.service.SkuAttrValueService;
import com.atguigu.gmall.pms.entity.vo.SaleAttrValueVo;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.stream.Collectors;


@Service("skuAttrValueService")
public class SkuAttrValueServiceImpl extends ServiceImpl<SkuAttrValueMapper, SkuAttrValueEntity> implements SkuAttrValueService {

    @Autowired
    private AttrMapper attrMapper;
    @Autowired
    private SkuMapper skuMapper;
    @Autowired
    private SkuAttrValueMapper skuAttrValueMapper;

    @Override
    public PageResultVo queryPage(PageParamVo paramVo) {
        IPage<SkuAttrValueEntity> page = this.page(
                paramVo.getPage(),
                new QueryWrapper<SkuAttrValueEntity>()
        );

        return new PageResultVo(page);
    }

    /**
     * ES搜索之根据分类Id、skuId查询检索类型的销售属性及值
     * ------------------------------------------------
     * 根据
     * 分类Id、
     * search_type=1【是否需要检索[0-不需要，1-需要]】、
     * skuId
     * 查询检索类型的销售属性及值
     * 查询的是 pms_attr 和 pms_sku_attr_value
     * <p>
     * 【联表查询SQL语句】
     * -- 查询A、B两表的交集，使用内连接INNER JOIN
     * SELECT t1.*
     * FROM pms_sku_attr_value t1
     * INNER JOIN pms_attr t2
     * ON t1.attr_id = t2.id
     * WHERE
     * t2.category_id = 225
     * AND
     * t2.search_type = 1
     * AND
     * t1.sku_id = 3
     * -- 但是一般不建议使用联表查询，而是使用分步查询
     * -- （1）先查询出：pms_attr表中search_type = 1 和 category_id = 225的数据
     * SELECT *
     * FROM pms_attr
     * WHERE
     * category_id = 225
     * AND
     * search_type = 1
     * -- （2）再查询出：再根据skuId、attrId，查询pms_sku_attr_value表中，符合t1.attr_id = t2.id的数据【但是这只是属于联表查询的ON】，这里只要指出范围即可
     * SELECT *
     * FROM pms_sku_attr_value
     * WHERE
     * sku_id = 3
     * AND
     * attr_id in (4,5,6,8,9)
     * -- 这样的分布查询，跟上面的联表查询结果是一样的
     */
    @Override
    public List<SkuAttrValueEntity> querySkuAttrValueByCidAndSkuId(Long cid, Long skuId) {
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
        //（3）第三步：执行第二个SQL查询语句，查询出销售检索类型的规格参数及值
        List<SkuAttrValueEntity> skuAttrValueEntityList = this.list(new QueryWrapper<SkuAttrValueEntity>().eq("sku_id", skuId).in("attr_id", attrIds));
        return skuAttrValueEntityList;
    }

    /**
     * 商品详情页接口八：查询sku信息后得到spuId，根据spuId查询spu下的所有销售属性
     * -------------------------------------------------------------------------------
     * pms_attr表中的type字段，属性类型[0-销售属性，1-基本属性，2-既是销售属性又是基本属性]
     * 查询销售属性[销售类型规格参数]，查询的是pms_sku_attr_value
     * 查询基本属性[规格参数]，查询的是pms_spu_attr_value
     * -------------------------------------------------------------------------------
     * 返回值是根据ItemVo的属性类型来定义
     * -------------------------------------------------------------------------------
     * 虽然最终查询的是pms_sku_attr_value的数据
     * 但还是需要联表pms_sku，获取到spu_id来查，sql语句如下：
     * SELECT a.*
     * FROM pms_sku_attr_value a
     * INNER JOIN pms_sku b
     * ON a.`sku_id` = b.`id`
     * WHERE  b.`spu_id` = 7;
     * -------------------------------------------------------------------------------
     * 如果不联表，只是根据sku_id来查，sql语句：
     * SELECT * FROM pms_sku_attr_value WHERE sku_id = 1;
     * 运行结果就只有单个sku下的销售属性
     * 而我们要查的是，spu下的每个sku的销售属性
     * -------------------------------------------------------------------------------
     * 有两种实现方式：
     * （1）第一种：直接在mapper的xml映射文件进行联表查询：SQL语句如下
     * SELECT a.* FROM pms_sku_attr_value a INNER JOIN pms_sku b ON a.`sku_id` = b.`id`
     * WHERE  b.`spu_id` = 7;
     * （2）第二种：也可以进行分步查询
     * 第一步是先查询出spu下的所有sku集合，sql语句如下：
     * SELECT * FROM pms_sku WHERE `spu_id` = 7;
     * 得到的sku集合
     * 第二步是再根据sku集合中的id集合，进行查询每个sku下的销售属性，sql语句如下：
     * SELECT * FROM pms_sku_attr_value WHERE `sku_id` in (1,2) ORDER BY attr_id ASC;
     * 这两种查询的结果是一样的，只不过第一种需要自定义SQL，第二种不需要
     * 第二步查询追加排序，方便后续查询skuId跟销售属性的映射关系
     */
    @Override
    public List<SaleAttrValueVo> querySkuAttrValuesBySpuId(Long spuId) {
        // 1.分步查询第一步：先查询出spu下的所有sku集合，查的是pms_sku表
        List<SkuEntity> skuEntityList =
                this.skuMapper.selectList(new QueryWrapper<SkuEntity>().eq("spu_id", spuId));
        /*
         2.第二步：再从sku集合中，获取出每个sku的id集合，使用stream流，将一个集合转换为另一个集合
         List<Long> skuIds =
                 skuEntityList.stream().map(skuEntity -> skuEntity.getId()).collect(Collectors.toList());
         skuEntity -> skuEntity.getId() 也可以写成 SkuEntity::getId
         注意大小写，前面使用对象变量名调用，后面使用对象调用
         */
        List<Long> skuIdList =
                skuEntityList.stream().map(SkuEntity::getId).collect(Collectors.toList());
        // 3.第三步：根据skuIdLit，查询出每个sku下的销售属性，查询的是pms_sku_attr_value表
        List<SkuAttrValueEntity> skuAttrValueEntityList =
                this.list(new QueryWrapper<SkuAttrValueEntity>().in("sku_id", skuIdList).orderByAsc("attr_id"));
        /*
        但此时我们查询出来的集合数据，是List<SkuAttrValueEntity>
        我们方法的返回值，是List<SaleAttrValueVo>
        前端需要的数据：
        [{attrId: 3,attrName: '颜色',attrValues: ['白色', '黑色', '粉色']},
         {attrId: 8,attrName: '内存',attrValues: ['6G', '8G', '12G']},
         {attrId: 9,attrName: '存储',attrValues: ['128G', '256G', '512G']}]
        是一组一组根据attrId来分组的对象
        4.第四步：使用stream流的collect的方法，对SkuAttrValueEntity的attrId来进行分组，得到一个map集合
        map的key就是attrId，value就是分组后的销售属性集合List<SkuAttrValueEntity>
         */
        Map<Long, List<SkuAttrValueEntity>> map =
                skuAttrValueEntityList.stream().collect(Collectors.groupingBy(SkuAttrValueEntity::getAttrId));
        // 6.第六步：创建一个集合List<SaleAttrValueVo>，用来存放从map集合遍历出来的每一个SaleAttrValueVo对象
        List<SaleAttrValueVo> saleAttrValueVoList = new ArrayList<>();
        /*
         5.第五步：对map进行遍历，取出每一组对象，放入到SaleAttrValueVo对象中
         map的key就是attrId，value就是分组后的销售属性集合List<SkuAttrValueEntity>
         */
        map.forEach((attrId, skuAttrValueEntityList2) -> {
            // 5.1.创建SaleAttrValueVo对象
            SaleAttrValueVo saleAttrValueVo = new SaleAttrValueVo();
            // 5.2.设置属性attrId
            saleAttrValueVo.setAttrId(attrId);
            // 5.3.设置属性attrName，只要有分组，分组就必然有且至少有一个元素，所以只要取第一个元素，就是attrName
            saleAttrValueVo.setAttrName(skuAttrValueEntityList2.get(0).getAttrName());
            /*
             5.4.设置属性attrValues
             skuAttrValueEntityList2是个list集合，是可以有重复对象
             而attrValues是set集合，是不可重复
             所以需要进行转换
             */
            Set<String> attrValues = skuAttrValueEntityList2.stream().map(SkuAttrValueEntity::getAttrValue).collect(Collectors.toSet());
            saleAttrValueVo.setAttrValues(attrValues);
            // 7.第七步：将每一个SaleAttrValueVo对象，放入到集合当中
            saleAttrValueVoList.add(saleAttrValueVo);
        });
        // 8.第八步：将List<SaleAttrValueVo>返回
        return saleAttrValueVoList;
    }

    /**
     * 商品详情页接口十：根据sku中的spuId查询spu下所有sku，以及所有sku的销售属性组合与skuId映射关系
     * ---------------------------------------------------------------------------------------
     * 实现的SQL语句如下：
     * SELECT sku_id,GROUP_CONCAT(attr_value ORDER BY attr_id ASC) AS attr_values
     * FROM pms_sku_attr_value
     * WHERE `sku_id` in (1,2)
     * GROUP BY sku_id;
     * GROUP_CONCAT表示将同一字段的数据会被拼接后存入同一字段中，并以相应的分隔符分隔
     */
    @Override
    public Map<String, Object> querySaleAttrsMappingSkuIdBySpuId(Long spuId) {
        // 1.第一步：先查询出spu下的所有sku集合，查的是pms_sku表
        List<SkuEntity> skuEntityList =
                this.skuMapper.selectList(new QueryWrapper<SkuEntity>().eq("spu_id", spuId));
        // 2.第二步：再从sku集合中，获取出每个sku的id集合，使用stream流，将一个集合转换为另一个集合
        List<Long> skuIdList =
                skuEntityList.stream().map(SkuEntity::getId).collect(Collectors.toList());
        // 3.第三步：根据skuIdLit，查询出每个sku下的销售属性以及对应的销售属性，需要自定义sql查询
        List<Map<String, Object>> mapList = this.skuAttrValueMapper.querySaleAttrsMappingSkuIdBySkuIds(skuIdList);
        /*
        4.第四步：将 List<Map<String, Object>> 转换为 Map<String, Object>
        要实现的效果：
        [{sku_id=1, attrValues=黑色,8G,128G}, {sku_id=2, attrValues=白色,8G,256G}]
        也就是以attrValues为map的key，要转换为String类型
        以sku_id为map的value，Object类型
         */
        Map<String, Object> mappingMap = mapList.stream().collect(Collectors.toMap(
                map -> map.get("attrValues").toString(),
                map -> map.get("sku_id")
        ));
        // 5.第五步：将映射好skuId和对应的销售属性的map集合mappingMap，返回取
        return mappingMap;
    }
}