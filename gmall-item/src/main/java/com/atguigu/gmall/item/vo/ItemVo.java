package com.atguigu.gmall.item.vo;

import com.atguigu.gmall.pms.entity.CategoryEntity;
import com.atguigu.gmall.pms.entity.SkuImagesEntity;
import com.atguigu.gmall.pms.entity.vo.ItemGroupVo;
import com.atguigu.gmall.pms.entity.vo.SaleAttrValueVo;
import com.atguigu.gmall.sms.vo.ItemSaleVo;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * @Description 商品详情页总的数据模型
 * @Author Austin
 * @Date 2021/3/23
 */
@Data
public class ItemVo {
    // 三级分类
    private List<CategoryEntity> categories;

    // 品牌
    private Long brandId;
    private String brandName;

    // spu
    private Long spuId;
    private String spuName;

    // sku
    private Long skuId;
    private String title;
    private String subTitle;
    private BigDecimal price;
    private Integer weight; //重量
    private String defaultImage;

    // 是否有货
    private Boolean store = false; // 默认无货

    // sku图片集合
    private List<SkuImagesEntity> images;

    // 营销信息
    private List<ItemSaleVo> sales;

    // spu下的所有sku的销售属性
    private List<SaleAttrValueVo> saleAttrs;

    // 当前sku的销售属性
    private Map<Long, String> saleAttr;

    // 销售属性和skuId的映射关系
    private Map<String, Object> skuJsons;

    // spu的海报信息，商品描述
    private List<String> spuImages;

    // 规格参数组及组下的规格参数(带值)
    private List<ItemGroupVo> groups;
}
