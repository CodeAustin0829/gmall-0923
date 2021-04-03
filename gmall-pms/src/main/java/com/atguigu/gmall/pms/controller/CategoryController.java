package com.atguigu.gmall.pms.controller;

import com.atguigu.gmall.common.bean.PageParamVo;
import com.atguigu.gmall.common.bean.PageResultVo;
import com.atguigu.gmall.common.bean.ResponseVo;
import com.atguigu.gmall.pms.entity.CategoryEntity;
import com.atguigu.gmall.pms.service.CategoryService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 商品三级分类
 *
 * @author Austin
 * @email 985474500@qq.com
 * @date 2021-03-08 23:37:03
 */
@Api(tags = "商品三级分类 管理")
@RestController
@RequestMapping("pms/category")
public class CategoryController {

    @Autowired
    private CategoryService categoryService;

    /**
     * 查询三级总分类，结合接口文档来进行编写
     */
    @GetMapping("/parent/{parentId}")
    public ResponseVo<List<CategoryEntity>> queryCategoryByParentId(@PathVariable("parentId") Long parentId) {
        //要查询的是List<CategoryEntity>，查询后返回的就是List<CategoryEntity>
        List<CategoryEntity> categoryEntities = this.categoryService.queryCategoryByParentId(parentId);
        //将得到的List<CategoryEntity>，封装到ResponseVo的data属性中，返回给前端
        return ResponseVo.ok(categoryEntities);
    }

    /**
     * 查询二级分类及其三级分类
     */
    @GetMapping("/cates/{pid}")
    public ResponseVo<List<CategoryEntity>> queryLevel2CategoryWithSubsByPid(@PathVariable("pid") Long pid) {
        List<CategoryEntity> categoryEntityList = this.categoryService.queryLevel2CategoryWithSubsByPid(pid);
        return ResponseVo.ok(categoryEntityList);
    }

    /**
     * 列表
     */
    @GetMapping
    @ApiOperation("分页查询")
    public ResponseVo<PageResultVo> queryCategoryByPage(PageParamVo paramVo) {
        PageResultVo pageResultVo = categoryService.queryPage(paramVo);

        return ResponseVo.ok(pageResultVo);
    }


    /**
     * 信息
     */
    @GetMapping("{id}")
    @ApiOperation("详情查询")
    public ResponseVo<CategoryEntity> queryCategoryById(@PathVariable("id") Long id) {
        CategoryEntity category = categoryService.getById(id);

        return ResponseVo.ok(category);
    }

    /**
     * 保存
     */
    @PostMapping
    @ApiOperation("保存")
    public ResponseVo<Object> save(@RequestBody CategoryEntity category) {
        categoryService.save(category);

        return ResponseVo.ok();
    }

    /**
     * 修改
     */
    @PostMapping("/update")
    @ApiOperation("修改")
    public ResponseVo update(@RequestBody CategoryEntity category) {
        categoryService.updateById(category);

        return ResponseVo.ok();
    }

    /**
     * 删除
     */
    @PostMapping("/delete")
    @ApiOperation("删除")
    public ResponseVo delete(@RequestBody List<Long> ids) {
        categoryService.removeByIds(ids);

        return ResponseVo.ok();
    }

    /**
     * 商品详情页接口二：根据三级分类id查询一二三级分类
     */
    @GetMapping("all/{cid3}")
    public ResponseVo<List<CategoryEntity>> queryCategoriesByCid3(@PathVariable("cid3") Long cid3) {
        // 在Service进行编写，查询返回List<CategoryEntity>
        List<CategoryEntity> categoryEntityList = this.categoryService.queryCategoriesByCid3(cid3);
        // 返回List<CategoryEntity>
        return ResponseVo.ok(categoryEntityList);
    }
}
