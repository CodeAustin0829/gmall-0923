package com.atguigu.gmall.pms.controller;

import com.atguigu.gmall.common.bean.PageParamVo;
import com.atguigu.gmall.common.bean.PageResultVo;
import com.atguigu.gmall.common.bean.ResponseVo;
import com.atguigu.gmall.pms.entity.SkuAttrValueEntity;
import com.atguigu.gmall.pms.service.SkuAttrValueService;
import com.atguigu.gmall.pms.entity.vo.SaleAttrValueVo;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * sku销售属性&值
 *
 * @author Austin
 * @email 985474500@qq.com
 * @date 2021-03-08 23:37:03
 */
@Api(tags = "sku销售属性&值 管理")
@RestController
@RequestMapping("pms/skuattrvalue")
public class SkuAttrValueController {

    @Autowired
    private SkuAttrValueService skuAttrValueService;

    /**
     * 商品详情页接口十：根据sku中的spuId查询spu下所有sku，以及所有sku的销售属性组合与skuId映射关系
     */
    @GetMapping("mapping/{spuId}")
    public ResponseVo<Map<String, Object>> querySaleAttrsMappingSkuIdBySpuId(@PathVariable("spuId") Long spuId) {
        Map<String, Object> map = this.skuAttrValueService.querySaleAttrsMappingSkuIdBySpuId(spuId);
        return ResponseVo.ok(map);
    }

    /**
     * 商品详情页接口九：根据skuId，查询当前sku的销售属性
     */
    @GetMapping("sku/{skuId}")
    public ResponseVo<List<SkuAttrValueEntity>> querySkuAttrValuesBySkuId(@PathVariable("skuId") Long skuId) {
        List<SkuAttrValueEntity> skuAttrValueEntityList =
                this.skuAttrValueService.list(new QueryWrapper<SkuAttrValueEntity>().eq("sku_id", skuId));
        return ResponseVo.ok(skuAttrValueEntityList);
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
     */
    @GetMapping("spu/{spuId}")
    public ResponseVo<List<SaleAttrValueVo>> querySkuAttrValuesBySpuId(@PathVariable("spuId") Long spuId) {
        List<SaleAttrValueVo> saleAttrValueVoList =
                this.skuAttrValueService.querySkuAttrValuesBySpuId(spuId);
        return ResponseVo.ok(saleAttrValueVoList);
    }

    /**
     * ES搜索之根据分类Id、skuId查询检索类型的销售属性及值
     * 分类id在路径中传递、skuId在请求参数中传递
     */
    @GetMapping("category/{cid}")
    public ResponseVo<List<SkuAttrValueEntity>> querySkuAttrValueByCidAndSkuId(@PathVariable("cid") Long cid, @RequestParam("skuId") Long skuId) {
        List<SkuAttrValueEntity> skuAttrValueEntities = this.skuAttrValueService.querySkuAttrValueByCidAndSkuId(cid, skuId);
        return ResponseVo.ok(skuAttrValueEntities);
    }

    /**
     * 列表
     */
    @GetMapping
    @ApiOperation("分页查询")
    public ResponseVo<PageResultVo> querySkuAttrValueByPage(PageParamVo paramVo) {
        PageResultVo pageResultVo = skuAttrValueService.queryPage(paramVo);

        return ResponseVo.ok(pageResultVo);
    }

    /**
     * 信息
     */
    @GetMapping("{id}")
    @ApiOperation("详情查询")
    public ResponseVo<SkuAttrValueEntity> querySkuAttrValueById(@PathVariable("id") Long id) {
        SkuAttrValueEntity skuAttrValue = skuAttrValueService.getById(id);

        return ResponseVo.ok(skuAttrValue);
    }

    /**
     * 保存
     */
    @PostMapping
    @ApiOperation("保存")
    public ResponseVo<Object> save(@RequestBody SkuAttrValueEntity skuAttrValue) {
        skuAttrValueService.save(skuAttrValue);

        return ResponseVo.ok();
    }

    /**
     * 修改
     */
    @PostMapping("/update")
    @ApiOperation("修改")
    public ResponseVo update(@RequestBody SkuAttrValueEntity skuAttrValue) {
        skuAttrValueService.updateById(skuAttrValue);

        return ResponseVo.ok();
    }

    /**
     * 删除
     */
    @PostMapping("/delete")
    @ApiOperation("删除")
    public ResponseVo delete(@RequestBody List<Long> ids) {
        skuAttrValueService.removeByIds(ids);

        return ResponseVo.ok();
    }

}
