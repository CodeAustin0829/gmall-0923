package com.atguigu.gmall.pms.mapper;


import com.atguigu.gmall.pms.entity.SkuAttrValueEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
 * sku销售属性&值
 *
 * @author Austin
 * @email 985474500@qq.com
 * @date 2021-03-08 23:37:03
 */
@Mapper
public interface SkuAttrValueMapper extends BaseMapper<SkuAttrValueEntity> {

    /**
     * 商品详情页接口十：根据sku中的spuId查询spu下所有sku，以及所有sku的销售属性组合与skuId映射关系
     * ---------------------------------------------------------------------------------------
     * 实现的SQL语句如下：
     * SELECT sku_id,GROUP_CONCAT(attr_value ORDER BY attr_id ASC) AS attr_values
     * FROM pms_sku_attr_value
     * WHERE `sku_id` in (1,2)
     * GROUP BY sku_id;
     * GROUP_CONCAT表示将同一字段的数据会被拼接后存入同一字段中，并以相应的分隔符分隔
     * ---------------------------------------------------------------------------------------
     * 【注意点】
     * （1）当参数是List类型，需要使用@Param来指定参数名，否则无法接收
     * （2）返回值用Map<String, Object>来接收也不行，会报错
     * 需要使用List<Map<String, Object>>，因为每返回一个sku销售属性和skuId的对应关系，就是一个Map
     * 而这样的对应关系有多个，所以需要用List来接收
     * （3）@Param注意不要导错包
     * import org.springframework.data.repository.query.Param; ❌
     * import org.apache.ibatis.annotations.Param; ✔
     */
    public List<Map<String, Object>> querySaleAttrsMappingSkuIdBySkuIds(@Param("skuIdList") List<Long> skuIdList);
    /**
     * 输出结果：
     * [{sku_id=1, attrValues=黑色,8G,128G}, {sku_id=2, attrValues=白色,8G,256G}]
     * 跟sql语句查询结果是一致的
     */
}
