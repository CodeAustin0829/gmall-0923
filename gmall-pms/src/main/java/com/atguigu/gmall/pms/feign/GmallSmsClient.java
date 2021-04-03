package com.atguigu.gmall.pms.feign;

import com.atguigu.gmall.common.bean.ResponseVo;
import com.atguigu.gmall.sms.api.GmallSmsApi;
import com.atguigu.gmall.sms.vo.SkuSaleVo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * @Description
 * @Author Austin
 * @Date 2021/3/11
 */
@FeignClient("sms-service")
public interface GmallSmsClient extends GmallSmsApi {
    /*
    已经从GmallSmsApi中继承
    @PostMapping("sms/skubounds/skusale/save")
    public ResponseVo<Object> saveSkuSale(@RequestBody SkuSaleVo skuSaleVo);
    */
}
