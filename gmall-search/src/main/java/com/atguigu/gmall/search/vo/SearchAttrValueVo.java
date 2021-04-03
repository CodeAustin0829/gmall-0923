package com.atguigu.gmall.search.vo;

import lombok.Data;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

@Data
public class SearchAttrValueVo {

    @Field(type = FieldType.Long)
    private Long attrId;//根据规格Id来聚合出规格参数
    @Field(type = FieldType.Keyword)
    private String attrName;//规格名称用来展示
    @Field(type = FieldType.Keyword)
    private String attrValue;//同时提供当前商品的规格参数值，聚合出可选值，让用户可选规格参数
}