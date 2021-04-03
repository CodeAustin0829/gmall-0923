package com.atguigu.gmall.pms.service.impl;

import com.atguigu.gmall.common.bean.PageParamVo;
import com.atguigu.gmall.common.bean.PageResultVo;
import com.atguigu.gmall.pms.entity.CategoryEntity;
import com.atguigu.gmall.pms.mapper.CategoryMapper;
import com.atguigu.gmall.pms.service.CategoryService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;


@Service("categoryService")
public class CategoryServiceImpl extends ServiceImpl<CategoryMapper, CategoryEntity> implements CategoryService {

    @Autowired
    private CategoryMapper categoryMapper;

    @Override
    public PageResultVo queryPage(PageParamVo paramVo) {
        IPage<CategoryEntity> page = this.page(
                paramVo.getPage(),
                new QueryWrapper<CategoryEntity>()
        );

        return new PageResultVo(page);
    }

    /**
     * 查询三级分类，结合接口文档来进行编写
     */
    @Override
    public List<CategoryEntity> queryCategoryByParentId(Long parentId) {

        //（2）第二步：提供查询需要的调教对象，queryWrapper
        QueryWrapper<CategoryEntity> queryWrapper = new QueryWrapper<>();

        //（3）第三步：对parentId进行判断
        /*
         根据接口文档，-1：查询所有，当前端传参传过来的parentId是-1，查询所有数据
         但是数据库表pms_category中，parentId并没有-1这个值
         所以，我们直接将-1设置到查询条件中，queryWrapper.eq("parent_id",parentId);
         到数据库是查不出数据的
         所以，只能对parentId进行判断
         当parentId=-1，就跳过if判断，不需要查询条件了【也就是queryWrapper为空】，直接去查，空条件就是查询所有数据
         而parentId != -1时，才需要查询条件，将parentId设置到条件中，进行具体查询
         */
        if (parentId != -1) {
            queryWrapper.eq("parent_id", parentId);
        }
        //（1）第一步：调用mybatis-plus提供的基类BaseMapper，查询数据库，CategoryMapper已经继承
        return this.categoryMapper.selectList(queryWrapper);
    }

    /**
     * 查询二级分类及其三级分类
     */
    @Override
    public List<CategoryEntity> queryLevel2CategoryWithSubsByPid(Long pid) {
        return this.categoryMapper.queryLevel2CategoryWithSubsByPid(pid);
    }

    /**
     * 商品详情页接口二：根据三级分类id查询一二三级分类
     */
    @Override
    public List<CategoryEntity> queryCategoriesByCid3(Long cid3) {
        // 1.先根据三级分类id，查询出三级分类对象
        CategoryEntity categoryEntity3 = this.categoryMapper.selectById(cid3);
        // 2.判断查询出的三级分类对象是否为空，因为用户在浏览器端穿过来的cid3在数据库中可能不存在
        if (categoryEntity3 == null) {
            return null;
        }
        // 3.三级分类对象中的父id，就是二级分类的id，可据此查询二级分类对象
        CategoryEntity categoryEntity2 = this.categoryMapper.selectById(categoryEntity3.getParentId());
        // 4.二级分类对象中的父id，就是一级分类的id，可据此查询一级分类对象
        CategoryEntity categoryEntity1 = this.categoryMapper.selectById(categoryEntity2.getParentId());
        // 将一二三级分类对象，放入到集合List<CategoryEntity>中返回
        return Arrays.asList(categoryEntity1, categoryEntity2, categoryEntity3);
    }

}