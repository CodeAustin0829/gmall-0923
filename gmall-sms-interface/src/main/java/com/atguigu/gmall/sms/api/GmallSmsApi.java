package com.atguigu.gmall.sms.api;

import com.atguigu.gmall.common.bean.ResponseVo;
import com.atguigu.gmall.sms.vo.ItemSaleVo;
import com.atguigu.gmall.sms.vo.SkuSaleVo;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

/**
 * @Description
 * @Author Austin
 * @Date 2021/3/11
 */
public interface GmallSmsApi {

    /**
     * 商品详情页接口六：根据skuId，查询sku的所有营销信息
     */
    @GetMapping("sms/skubounds/sku/{skuId}")
    public ResponseVo<List<ItemSaleVo>> querySalesBySkuId(@PathVariable("skuId") Long skuId);

    /**
     * 新增sku的营销信息
     */
    @PostMapping("sms/skubounds/skusale/save")
    public ResponseVo<Object> saveSkuSale(@RequestBody SkuSaleVo skuSaleVo);
}
