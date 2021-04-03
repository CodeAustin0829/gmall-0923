package com.atguigu.gmall.pms.controller;

import com.atguigu.gmall.common.bean.PageParamVo;
import com.atguigu.gmall.common.bean.PageResultVo;
import com.atguigu.gmall.common.bean.ResponseVo;
import com.atguigu.gmall.pms.entity.AttrGroupEntity;
import com.atguigu.gmall.pms.entity.vo.GroupVo;
import com.atguigu.gmall.pms.entity.vo.ItemGroupVo;
import com.atguigu.gmall.pms.service.AttrGroupService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 属性分组
 *
 * @author Austin
 * @email 985474500@qq.com
 * @date 2021-03-08 23:37:04
 */
@Api(tags = "属性分组 管理")
@RestController
@RequestMapping("pms/attrgroup")
public class AttrGroupController {

    @Autowired
    private AttrGroupService attrGroupService;

    /**
     * 商品详情页接口十二：根据分类id、spuId及skuId，查询分组及组下的规格参数值
     */
    @GetMapping("group/withAttrValues/{cid}")
    public ResponseVo<List<ItemGroupVo>> queryGroupWithAttrValuesByCidAndSpuIdAndSkuId(
            @PathVariable("cid") Long cid,
            @RequestParam("spuId") Long spuId,
            @RequestParam("skuId") Long skuId) {
        List<ItemGroupVo> itemGroupVoList =
                this.attrGroupService.queryGroupWithAttrValuesByCidAndSpuIdAndSkuId(cid, spuId, skuId);
        return ResponseVo.ok(itemGroupVoList);
    }

    /**
     * 当点击第三级分类，要查询属性分组信息时，浏览器发送的请求：
     * Request URL: http://api.gmall.com/pms/attrgroup/category/225
     * Request Method: GET
     * 通过此，可以编写后端接口：
     * （1）请求方式：GET
     * （2）请求路径：pms/attrgroup/category/225，也就是category/{cid}
     * （3）请求参数：225，也就是数据表pms_attr_group中的字段分组category_id
     * （4）返回值：返回一个ResponseVo，泛型是List<AttrGroupEntity>
     */
    @GetMapping("/category/{cid}")
    public ResponseVo<List<AttrGroupEntity>> queryAttrGroupByCid(@PathVariable("cid") Long cid) {
        //（2）第二步：提供查询条件参数
        QueryWrapper<AttrGroupEntity> queryWrapper = new QueryWrapper<>();
        //设置查询条件，条件是前端传过来的cid，等于pms_attr_group中的字段分组category_id，
        queryWrapper.eq("category_id", cid);

        //（1）第一步：调用mybatis-plus的基类IService提供的查询list方法
        List<AttrGroupEntity> attrGroupEntityList = attrGroupService.list(queryWrapper);

        //（3）第三步：将查询到的List<AttrGroupEntity>，封装到ResponseVo的data属性，返回给前端
        return ResponseVo.ok(attrGroupEntityList);
    }

    /**
     * 商品新增之查询分类下的分组及其规格参数
     * 当前端页面点击【商品列表\添加spu\选择分类，选择到第三分类的时候】
     * 前端会向后端发送请求：
     * Request URL: http://api.gmall.com/pms/attrgroup/withattrs/225
     * Request Method: GET
     * -------------------------------------------------------------
     * 根据分析接口文档，返回值是ResponseVo<List<AttrGroupEntity>>
     * 但是AttrGroupEntity中没有相应的attrEntities属性
     * 所以编写一个扩展类GroupVo继承AttrGroupEntity，然后扩展一个attrEntities
     * attrEntities里面，又封装了AttrEntity的集合
     * 所以属性是 List<AttrEntity> attrEntities
     * 然后返回值是ResponseVo<List<GroupVo>>
     */
    @GetMapping("withattrs/{catId}")
    public ResponseVo<List<GroupVo>> queryGroupVoByCid(@PathVariable("catId") Long cid) {
        //要查询的是List<GroupVo>，查询后返回的就是List<GroupVo>
        List<GroupVo> groupVoList = attrGroupService.queryGroupVoByCid(cid);
        //将得到的List<GroupVo>，封装到ResponseVo的data属性中，返回给前端
        return ResponseVo.ok(groupVoList);
    }

    /**
     * 列表
     */
    @GetMapping
    @ApiOperation("分页查询")
    public ResponseVo<PageResultVo> queryAttrGroupByPage(PageParamVo paramVo) {
        PageResultVo pageResultVo = attrGroupService.queryPage(paramVo);

        return ResponseVo.ok(pageResultVo);
    }

    /**
     * 信息
     */
    @GetMapping("{id}")
    @ApiOperation("详情查询")
    public ResponseVo<AttrGroupEntity> queryAttrGroupById(@PathVariable("id") Long id) {
        AttrGroupEntity attrGroup = attrGroupService.getById(id);

        return ResponseVo.ok(attrGroup);
    }

    /**
     * 保存
     */
    @PostMapping
    @ApiOperation("保存")
    public ResponseVo<Object> save(@RequestBody AttrGroupEntity attrGroup) {
        attrGroupService.save(attrGroup);

        return ResponseVo.ok();
    }

    /**
     * 修改
     */
    @PostMapping("/update")
    @ApiOperation("修改")
    public ResponseVo update(@RequestBody AttrGroupEntity attrGroup) {
        attrGroupService.updateById(attrGroup);

        return ResponseVo.ok();
    }

    /**
     * 删除
     */
    @PostMapping("/delete")
    @ApiOperation("删除")
    public ResponseVo delete(@RequestBody List<Long> ids) {
        attrGroupService.removeByIds(ids);

        return ResponseVo.ok();
    }

}
