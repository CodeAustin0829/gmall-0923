package com.atguigu.gmall.pms.api;

import com.atguigu.gmall.common.bean.PageParamVo;
import com.atguigu.gmall.common.bean.ResponseVo;
import com.atguigu.gmall.pms.entity.*;
import com.atguigu.gmall.pms.entity.vo.ItemGroupVo;
import com.atguigu.gmall.pms.entity.vo.SaleAttrValueVo;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * @Description gmall-search进行远程调用的接口方法
 * @Author Austin
 * @Date 2021/3/15
 */
public interface GmallPmsApi {

    /**
     * （1）ES搜索之分页查询spu
     */
    @PostMapping("pms/spu/Page")
    public ResponseVo<List<SpuEntity>> querySpuPage(@RequestBody PageParamVo paramVo);

    /**
     * （2）ES搜索之根据spuId，查询spu下的所有sku信息
     */
    @GetMapping("pms/sku/spu/{spuId}")
    public ResponseVo<List<SkuEntity>> querySkuBySpuId(@PathVariable("spuId") Long spuId);

    //（3）在gmall-wms-interface中

    /**
     * （4）ES搜索之根据品牌Id，查询品牌信息
     */
    @GetMapping("pms/brand/{id}")
    public ResponseVo<BrandEntity> queryBrandById(@PathVariable("id") Long id);

    /**
     * （5）ES搜索之根据分类Id，查询分类信息
     */
    @GetMapping("pms/category/{id}")
    public ResponseVo<CategoryEntity> queryCategoryById(@PathVariable("id") Long id);

    /**
     * （6）ES搜索之根据分类Id、skuId查询检索类型的销售属性及值
     * 分类id在路径中传递、skuId在请求参数中传递
     */
    @GetMapping("pms/skuattrvalue/category/{cid}")
    public ResponseVo<List<SkuAttrValueEntity>> querySkuAttrValueByCidAndSkuId(@PathVariable("cid") Long cid, @RequestParam("skuId") Long skuId
    );

    /**
     * （7）ES搜索之根据分类Id、spuId查询检索类型的基本属性及值
     * 分类id在路径中传递、spuId在请求参数中传递
     */
    @GetMapping("pms/spuattrvalue/category/{cid}")
    public ResponseVo<List<SpuAttrValueEntity>> querySpuAttrValueByCidAndSpuId(@PathVariable("cid") Long cid, @RequestParam("spuId") Long spuId
    );

    //根据spuId，查询spuEntity
    @GetMapping("pms/spu/{id}")
    @ApiOperation("详情查询")
    public ResponseVo<SpuEntity> querySpuById(@PathVariable("id") Long id);

    //根据父id，查询出分类集合，只要传parentId=0过来，查询的就是一级分类
    @GetMapping("pms/category/parent/{parentId}")
    public ResponseVo<List<CategoryEntity>> queryCategoryByParentId(@PathVariable("parentId") Long parentId);

    /**
     * 查询二级分类及其三级分类
     */
    @GetMapping("pms/category/cates/{pid}")
    public ResponseVo<List<CategoryEntity>> queryLevel2CategoryWithSubsByPid(@PathVariable("pid") Long pid);

    /**
     * 商品详情页接口一：根据skuId，查询sku信息
     */
    @GetMapping("pms/sku/{id}")
    public ResponseVo<SkuEntity> querySkuById(@PathVariable("id") Long id);

    /**
     * 商品详情页接口二：查询sku信息后得到三级分类id，根据三级分类id查询一二三级分类
     */
    @GetMapping("pms/category/all/{cid3}")
    public ResponseVo<List<CategoryEntity>> queryCategoriesByCid3(@PathVariable("cid3") Long cid3);

    /**
     * 商品详情页接口三：查询sku信息后得到品牌id，根据品牌id查询品牌，上面已引用
     */

    /**
     * 商品详情页接口四：查询sku信息后得到spuId，根据spuId查询spu信息，上面已引用
     */

    /**
     * 商品详情页接口五：根据skuId，查询sku所有图片
     */
    @GetMapping("pms/skuimages/sku/{skuId}")
    public ResponseVo<List<SkuImagesEntity>> querySkuImagesBySkuId(@PathVariable("skuId") Long skuId);

    /**
     * 商品详情页接口六：根据skuId，查询sku的所有营销信息，在sms微服务中
     */

    /**
     * 商品详情页接口七：根据skuId，查询sku的库存信息，在wms微服务中
     */

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
     */
    @GetMapping("pms/skuattrvalue/spu/{spuId}")
    public ResponseVo<List<SaleAttrValueVo>> querySkuAttrValuesBySpuId(@PathVariable("spuId") Long spuId);

    /**
     * 商品详情页接口九：根据skuId，查询当前sku的销售属性
     */
    @GetMapping("pms/skuattrvalue/sku/{skuId}")
    public ResponseVo<List<SkuAttrValueEntity>> querySkuAttrValuesBySkuId(@PathVariable("skuId") Long skuId);

    /**
     * 商品详情页接口十：根据sku中的spuId查询spu下所有sku，以及所有sku的销售属性组合与skuId映射关系
     */
    @GetMapping("pms/skuattrvalue/mapping/{spuId}")
    public ResponseVo<Map<String, Object>> querySaleAttrsMappingSkuIdBySpuId(@PathVariable("spuId") Long spuId);

    /**
     * 商品详情页接口十一：查询sku信息后得到spuId，根据sku中spuId查询spu的描述信息
     */
    @GetMapping("pms/spudesc/{spuId}")
    public ResponseVo<SpuDescEntity> querySpuDescById(@PathVariable("spuId") Long spuId);

    /**
     * 商品详情页接口十二：根据分类id、spuId及skuId，查询分组及组下的规格参数值
     */
    @GetMapping("pms/attrgroup/group/withAttrValues/{cid}")
    public ResponseVo<List<ItemGroupVo>> queryGroupWithAttrValuesByCidAndSpuIdAndSkuId(@PathVariable("cid") Long cid, @RequestParam("spuId") Long spuId, @RequestParam("skuId") Long skuId);
}
