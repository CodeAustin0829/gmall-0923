package com.atguigu.gmall.search.vo;

import lombok.Data;

import java.util.List;

/**
 * 接受页面传递过来的检索参数：
 * 类似的路径分析：
 * http://search.gmall.com/search?keyword=手机&brandId=1,3&cid=225&props=5:8-12,5:256G&sort=1&priceFrom=1000&priceTo=6000&pageNum=1&store=true
 */
@Data
public class SearchParamVo {
    // 关键字检索条件
    private String keyword;
    // 品牌过滤
    private List<Long> brandId;
    // 分类过滤
    private List<Long> cid;
    // 检索参数过滤
    // props=5:8-12,5:256G
    private List<String> props;
    // 排序字段：0-默认，得分降序；1-按价格升序；2-按价格降序；3-按创建时间降序；4-按销量降序
    private Integer sort = 0;
    // 价格区间
    private Double priceFrom;//起始价格
    private Double priceTo;//结束价格
    // 页码，默认是第一页
    private Integer pageNum = 1;
    // 每页记录数，规定死页面每页显示20条数据，不允许修改，所以使用final
    private final Integer pageSize = 20;
    // 是否有货
    private Boolean store;
}