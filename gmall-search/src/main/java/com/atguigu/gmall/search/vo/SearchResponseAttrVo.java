package com.atguigu.gmall.search.vo;

import lombok.Data;

import java.util.List;

/**
 * @Description
 * @Author Austin
 * @Date 2021/3/16
 */
@Data
public class SearchResponseAttrVo {
    private Long attrId;
    private String attrName;
    private List<String> attrValues;
}
