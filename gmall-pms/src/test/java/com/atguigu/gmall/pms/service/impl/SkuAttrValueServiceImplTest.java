package com.atguigu.gmall.pms.service.impl;

import com.atguigu.gmall.pms.service.SkuAttrValueService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * @Description
 * @Author Austin
 * @Date 2021/3/24
 */
@SpringBootTest
class SkuAttrValueServiceImplTest {

    @Autowired
    private SkuAttrValueService skuAttrValueService;

    @Test
    void querySaleAttrsMappingSkuIdBySpuId() {
        System.out.println(this.skuAttrValueService.querySaleAttrsMappingSkuIdBySpuId(7L));
        /**
         * 输出结果：{黑色,8G,128G=1, 白色,8G,256G=2}
         */
    }
}