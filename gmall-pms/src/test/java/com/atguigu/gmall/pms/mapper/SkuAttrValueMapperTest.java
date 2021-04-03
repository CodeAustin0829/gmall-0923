package com.atguigu.gmall.pms.mapper;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Arrays;

/**
 * @Description
 * @Author Austin
 * @Date 2021/3/24
 */
@SpringBootTest
class SkuAttrValueMapperTest {

    @Autowired
    private SkuAttrValueMapper skuAttrValueMapper;

    @Test
    void querySaleAttrsMappingSkuIdBySkuIds() {
        System.out.println(this.skuAttrValueMapper.querySaleAttrsMappingSkuIdBySkuIds(Arrays.asList(1L, 2L)));
    }
}