package com.atguigu.gmall.pms.entity.vo;

import lombok.Data;

import java.util.Set;

/**
 * @Description
 * @Author Austin
 * @Date 2021/3/23
 */
@Data
public class SaleAttrValueVo {
    private Long attrId;
    private String attrName;
    private Set<String> attrValues;
}
