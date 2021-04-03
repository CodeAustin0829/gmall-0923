package com.atguigu.gmall.pms.controller;

import java.util.List;

import com.atguigu.gmall.pms.entity.AttrEntity;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.atguigu.gmall.pms.service.AttrService;
import com.atguigu.gmall.common.bean.PageResultVo;
import com.atguigu.gmall.common.bean.ResponseVo;
import com.atguigu.gmall.common.bean.PageParamVo;

/**
 * 商品属性
 *
 * @author Austin
 * @email 985474500@qq.com
 * @date 2021-03-08 23:37:04
 */
@Api(tags = "商品属性 管理")
@RestController
@RequestMapping("pms/attr")
public class AttrController {

    @Autowired
    private AttrService attrService;

    /**
     * 查询分组下的规格参数
     * 前端发送的请求：
     * Request URL: http://api.gmall.com/pms/attr/group/1
     * Request Method: GET
     * 通过此，可以编写后端接口：
     * （1）请求方式：GET
     * （2）请求路径：pms/attr/group/1，也就是group/{gid}
     * （3）请求参数：1，也就是数据表pms_attr中的字段规格分组group_id
     * （4）返回值：返回一个ResponseVo，泛型是List<AttrEntity>
     */
    @GetMapping("/group/{gid}")
    public ResponseVo<List<AttrEntity>> queryAttrByGroupId(@PathVariable("gid") Long gid) {
        //（2）第二步：提供查询条件参数
        QueryWrapper<AttrEntity> queryWrapper = new QueryWrapper<>();
        //设置查询条件，条件是前端传过来的cid，等于pms_attr中的字段规格分组group_id，
        queryWrapper.eq("group_id", gid);

        //（1）第一步：调用mybatis-plus的基类IService提供的查询list方法
        List<AttrEntity> attrEntityList = attrService.list(queryWrapper);

        //（3）第三步：将查询到的List<AttrGroupEntity>，封装到ResponseVo的data属性，返回给前端
        return ResponseVo.ok(attrEntityList);
    }

    /**
     * 查询分类下的规格参数，用来录入sku相关信息
     * Request URL: http://api.gmall.com/pms/attr/category/225?type=0
     * Request Method: GET
     * -------------------------------------------------------------------------
     * 分析接口文档：
     * 请求地址：http://api.gmall.com/pms/attr/category/{cid}?type=0&searchType=1
     * 多出来一个检索参数，为了日后检索使用
     * 返回值：ResponseVo<List<AttrEntity>>
     */
    @GetMapping("category/{cid}")
    public ResponseVo<List<AttrEntity>> queryAttrByCIdOrTypeOrSearchType(
            @PathVariable Long cid, //接收Url中的参数
            @RequestParam(value = "type", required = false) Integer type, //接收？后中的参数
            @RequestParam(value = "searchType", required = false) Integer searchType //接收？后中的参数
            /*
            但是这样会出现一个问题，就是前端发送的请求当中，是没有searchType这个参数的，这样会报404
            因为每个参数都是默认required = true，也就是必须传参
            解决方案：
            （1）优先选择设置默认值，defaultValue = xxx
            defaultValue是String类型的
            （2）required = false:可以不传递
             */
    ) {
        List<AttrEntity> attrEntityList = attrService.queryAttrByCIdOrTypeOrSearchType(cid, type, searchType);
        return ResponseVo.ok(attrEntityList);
    }


    /**
     * 列表
     */
    @GetMapping
    @ApiOperation("分页查询")
    public ResponseVo<PageResultVo> queryAttrByPage(PageParamVo paramVo) {
        PageResultVo pageResultVo = attrService.queryPage(paramVo);

        return ResponseVo.ok(pageResultVo);
    }


    /**
     * 信息
     */
    @GetMapping("{id}")
    @ApiOperation("详情查询")
    public ResponseVo<AttrEntity> queryAttrById(@PathVariable("id") Long id) {
        AttrEntity attr = attrService.getById(id);

        return ResponseVo.ok(attr);
    }

    /**
     * 保存
     */
    @PostMapping
    @ApiOperation("保存")
    public ResponseVo<Object> save(@RequestBody AttrEntity attr) {
        attrService.save(attr);

        return ResponseVo.ok();
    }

    /**
     * 修改
     */
    @PostMapping("/update")
    @ApiOperation("修改")
    public ResponseVo update(@RequestBody AttrEntity attr) {
        attrService.updateById(attr);

        return ResponseVo.ok();
    }

    /**
     * 删除
     */
    @PostMapping("/delete")
    @ApiOperation("删除")
    public ResponseVo delete(@RequestBody List<Long> ids) {
        attrService.removeByIds(ids);

        return ResponseVo.ok();
    }

}
