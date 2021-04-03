package com.atguigu.gmall.oms.vo;

import com.atguigu.gmall.pms.entity.SkuAttrValueEntity;
import com.atguigu.gmall.sms.vo.ItemSaleVo;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * @Description 送货清单VO类
 * @Author Austin
 * @Date 2021/3/31
 */
@Data
public class OrderItemVo {
    private Long skuId; //点击标题的时候，可以跳转商品详情页，所以需要skuId属性
    private String title;
    private String defaultImage;
    private BigDecimal price;
    private Integer count;
    private BigDecimal weight;
    private List<SkuAttrValueEntity> saleAttrs; // 销售属性
    private List<ItemSaleVo> sales; // 营销信息
    private Boolean store = false; // 库存信息
}
