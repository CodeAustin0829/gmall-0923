package com.atguigu.gmall.pms.entity.vo;


import com.atguigu.gmall.pms.entity.SkuAttrValueEntity;
import com.atguigu.gmall.pms.entity.SkuEntity;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * @Description
 * @Author Austin
 * @Date 2021/3/10
 */
@Data
public class SkuVo extends SkuEntity {
    //（1）sku图片信息，保存在pms_sku_images表
    private List<String> images;

    //（2）积分信息，保存在sms_sku_bounds表
    /**
     * 成长积分
     */
    private BigDecimal growBounds;
    /**
     * 购物积分
     */
    private BigDecimal buyBounds;
    /**
     * 优惠生效情况
     * [1111（四个状态位，从右到左）;
     * 0 - 无优惠，成长积分是否赠送;
     * 1 - 无优惠，购物积分是否赠送;
     * 2 - 有优惠，成长积分是否赠送;
     * 3 - 有优惠，购物积分是否赠送
     * 【状态位0：不赠送，1：赠送】]
     */
    private List<Integer> work;


    //（3）打折信息，保存在sms_sku_ladder表
    /**
     * 满几件
     */
    private Integer fullCount;
    /**
     * 打几折
     */
    private BigDecimal discount;
    /**
     * 是否叠加其他优惠[0-不可叠加，1-可叠加]
     */
    private Integer ladderAddOther;


    //（4）叠加优惠信息，保存在sms_sku_full_reduction表
    /**
     * 满多少
     */
    private BigDecimal fullPrice;
    /**
     * 减多少
     */
    private BigDecimal reducePrice;
    /**
     * 是否参与其他优惠
     */
    private Integer fullAddOther;

    // sku的销售属性
    private List<SkuAttrValueEntity> saleAttrs;

}
