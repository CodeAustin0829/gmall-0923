package com.atguigu.gmall.sms.controller;

import com.atguigu.gmall.common.bean.PageParamVo;
import com.atguigu.gmall.common.bean.PageResultVo;
import com.atguigu.gmall.common.bean.ResponseVo;
import com.atguigu.gmall.sms.entity.SkuBoundsEntity;
import com.atguigu.gmall.sms.service.SkuBoundsService;
import com.atguigu.gmall.sms.vo.ItemSaleVo;
import com.atguigu.gmall.sms.vo.SkuSaleVo;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 商品spu积分设置
 *
 * @author Austin
 * @email 985474500@qq.com
 * @date 2021-03-09 00:14:24
 */
@Api(tags = "商品spu积分设置 管理")
@RestController
@RequestMapping("sms/skubounds")
public class SkuBoundsController {

    @Autowired
    private SkuBoundsService skuBoundsService;

    /**
     * 商品详情页接口六：根据skuId，查询sku的所有营销信息
     */
    @GetMapping("sku/{skuId}")
    public ResponseVo<List<ItemSaleVo>> querySalesBySkuId(@PathVariable("skuId") Long skuId) {
        // 需要查询guli_sms中的三张表:sms_sku_bounds积分表、sms_sku_full_reduction满减表、sms_sku_ladder打折表
        List<ItemSaleVo> itemSaleVoList = this.skuBoundsService.querySalesBySkuId(skuId);
        // 将查询到的三张表数据，返回
        return ResponseVo.ok(itemSaleVoList);
    }

    /**
     * 新增sku的营销信息
     */
    /**
     * feign远程调用，使用的是Json【Json只支持Post请求】才能传对象，？后传参在feign中不能使用对象类接收
     * 请求路径随意写
     * 数据库新增操作，不需要返回数据给前端，所以方法返回值也可以定义一个Object
     */
    @ApiOperation("新增sku的营销信息")
    @PostMapping("/skusale/save")
    public ResponseVo<Object> saveSkuSale(@RequestBody SkuSaleVo skuSaleVo) {
        //前端传过来的数据，封装在SkuSaleVo类中，封装的是sms_sku_bounds、sms_sku_ladder、sms_sku_full_reduction三张表的信息
        //所以还需要在Service进行拆分，分别存入对应的表中
        this.skuBoundsService.saveSkuSale(skuSaleVo);
        return ResponseVo.ok();
    }

    /**
     * 列表
     */
    @GetMapping
    @ApiOperation("分页查询")
    public ResponseVo<PageResultVo> querySkuBoundsByPage(PageParamVo paramVo) {
        PageResultVo pageResultVo = skuBoundsService.queryPage(paramVo);

        return ResponseVo.ok(pageResultVo);
    }

    /**
     * 信息
     */
    @GetMapping("{id}")
    @ApiOperation("详情查询")
    public ResponseVo<SkuBoundsEntity> querySkuBoundsById(@PathVariable("id") Long id) {
        SkuBoundsEntity skuBounds = skuBoundsService.getById(id);

        return ResponseVo.ok(skuBounds);
    }

    /**
     * 保存
     */
    @PostMapping
    @ApiOperation("保存")
    public ResponseVo<Object> save(@RequestBody SkuBoundsEntity skuBounds) {
        skuBoundsService.save(skuBounds);

        return ResponseVo.ok();
    }

    /**
     * 修改
     */
    @PostMapping("/update")
    @ApiOperation("修改")
    public ResponseVo update(@RequestBody SkuBoundsEntity skuBounds) {
        skuBoundsService.updateById(skuBounds);

        return ResponseVo.ok();
    }

    /**
     * 删除
     */
    @PostMapping("/delete")
    @ApiOperation("删除")
    public ResponseVo delete(@RequestBody List<Long> ids) {
        skuBoundsService.removeByIds(ids);

        return ResponseVo.ok();
    }

}
