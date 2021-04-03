package com.atguigu.gmall.search.vo;

import com.atguigu.gmall.pms.entity.BrandEntity;
import com.atguigu.gmall.pms.entity.CategoryEntity;
import com.atguigu.gmall.search.pojo.Goods;
import lombok.Data;

import java.util.List;

/**
 * @Description 封装搜索结果集，响应给前端页面
 * @Author Austin
 * @Date 2021/3/16
 */
@Data
public class SearchResponseVo {

    //（1）品牌过滤条件，使用现有的逆向工程生成的BrandEntity
    private List<BrandEntity> brands;

    //（2）分类过滤条件，使用现有的逆向工程生成的CategoryEntity
    private List<CategoryEntity> categories;

    //（3）规格参数过滤条件，创建一个Vo类来封装属性
    private List<SearchResponseAttrVo> filters;

    //（4）分页过滤条件
    private Integer pageNum;
    private Integer pageSize;
    private Long total;

    //（5）商品列表的数据，也就是封装了从数据库查询到的数据保存到ES的数据的Goods集合，也就是sku集合
    private List<Goods> goodsList;

}
